package com.samczsun.skype4j.internal;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;

import com.eclipsesource.json.JsonObject;
import com.samczsun.skype4j.chat.ChatMessage;
import com.samczsun.skype4j.chat.IndividualChat;
import com.samczsun.skype4j.exceptions.SkypeException;
import com.samczsun.skype4j.formatting.Text;
import com.samczsun.skype4j.user.User;

public class ChatIndividual extends ChatImpl implements IndividualChat {
    protected ChatIndividual(SkypeImpl skype, String identity) throws SkypeException {
        super(skype, identity);
    }
    
    @Override
    protected void load() {
        if (isLoaded()) {
            return;
        }
        isLoading.set(true);
        Map<String, User> newUsers = new HashMap<>();
        String username = this.getIdentity().substring(2);
        User user = users.get(username);
        if (user == null) {
            user = new UserImpl(username, this);
        }
        newUsers.put(username, user);
        User me = users.get(getClient().getUsername());
        if (me == null) {
            me = new UserImpl(getClient().getUsername(), this);
            newUsers.put(getClient().getUsername(), me);
        }
        this.users.clear();
        this.users.putAll(newUsers);
        isLoading.set(false);
        hasLoaded.set(true);
    }

    @Override
    public ChatMessage sendMessage(Text message) throws SkypeException {
        HttpsURLConnection con = null;
        try {
            long ms = System.currentTimeMillis();
            JsonObject obj = new JsonObject();
            obj.add("content", message.parent().write());
            obj.add("messagetype", "RichText");
            obj.add("contenttype", "text");
            obj.add("clientmessageid", String.valueOf(ms));
            URL url = new URL("https://client-s.gateway.messenger.live.com/v1/users/ME/conversations/" + this.getIdentity() + "/messages");
            con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("RegistrationToken", getClient().getRegistrationToken());
            con.setRequestProperty("Content-Type", "application/json");
            con.getOutputStream().write(obj.toString().getBytes(Charset.forName("UTF-8")));
            con.getInputStream();
            return ChatMessageImpl.createMessage(this, getUser(getClient().getUsername()), null, String.valueOf(ms), ms, Jsoup.parse(message.parent().write()).text());
        } catch (IOException e) {
            throw new SkypeException("An exception occured while sending a message", e);
        }
    }

    @Override
    public Collection<User> getAllUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    public void addUser(String username) {
        if (!users.containsKey(username)) {
            User user = new UserImpl(username, this);
            users.put(username, user);
        } else {
            System.out.println(username + " joined twice???");
        }
    }

    public void removeUser(String username) {
        users.remove(username);
    }

    public void onMessage(ChatMessage message) {
        this.messages.add(message);
        ((UserImpl) message.getSender()).onMessage(message);
    }

    @Override
    public User getUser(String username) {
        return this.users.get(username);
    }

    @Override
    public List<ChatMessage> getAllMessages() {
        return Collections.unmodifiableList(messages);
    }
}
