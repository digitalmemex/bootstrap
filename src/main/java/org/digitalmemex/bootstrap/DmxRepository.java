package org.digitalmemex.bootstrap;

import de.deepamehta.core.ChildTopics;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.Topic;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class DmxRepository implements JSONEnabled {

    private ChildTopics topics;

    public DmxRepository() {
    }

    public DmxRepository(Topic topic) {
        String typeUri = topic.getTypeUri();
        if (typeUri.equals("dmx.repository")) {
            this.topics = topic.loadChildTopics().getChildTopics();
        } else {
            throw new IllegalArgumentException("unsupported topic of type " + typeUri);
        }
    }

    public String getName() {
        return topics.getString("dmx.repository.name");
    }

    public String getDescription() {
        return topics.getString("dmx.repository.description");
    }

    public String getUri() {
        return topics.getString("dmx.repository.uri");
    }

    public String getBranch() {
        if (isInstalled() || topics.has("dmx.repository.branch")) {
            return topics.getString("dmx.repository.branch");
        } else {
            return "";
        }
    }

    public void setBranch(String branch) {
        topics.set("dmx.repository.branch", branch);
    }

    public String getHead() {
        if (isInstalled() || topics.has("dmx.repository.head")) {
            return topics.getString("dmx.repository.head");
        } else {
            return "";
        }
    }

    public void setHead(String head) {
        topics.set("dmx.repository.head", head);
    }

    public String getStatus() {
        if (topics.has("dmx.repository.status")) {
            return topics.getString("dmx.repository.status");
        } else {
            return "";
        }
    }

    public void setStatusConfigured() {
        topics.setRef("dmx.repository.status", "dmx.repository.status.configured");
    }

    public void setStatusInstalled() {
        topics.setRef("dmx.repository.status", "dmx.repository.status.installed");
    }

    public boolean isConfigured() {
        return getStatus().equals("Configured");
    }

    public boolean isInstalled() {
        return getStatus().equals("Installed");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject repo = new JSONObject();
        try {
            repo.put("name", getName());
            repo.put("description", getDescription());
            repo.put("uri", getUri());
            repo.put("branch", getBranch());
            repo.put("head", getHead());
            repo.put("status", getStatus());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return repo;
    }

}
