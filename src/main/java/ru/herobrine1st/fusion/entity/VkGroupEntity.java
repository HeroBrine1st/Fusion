package ru.herobrine1st.fusion.entity;


import org.hibernate.Session;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Entity
@Table(name = "vk_groups")
public class VkGroupEntity {
    @Id
    @Column(nullable = false)
    private long id; // id directly from VK API

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String avatarUrl;

    @Column(nullable = false)
    private long lastWallPostId; // To avoid duplicates

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true, mappedBy = "group")
    private List<VkGroupSubscriberEntity> subscribers;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public long getLastWallPostId() {
        return lastWallPostId;
    }

    public void setLastWallPostId(long lastWallPostId) {
        this.lastWallPostId = lastWallPostId;
    }

    public List<VkGroupSubscriberEntity> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<VkGroupSubscriberEntity> subscribers) {
        this.subscribers = subscribers;
    }

    public void addSubscriber(VkGroupSubscriberEntity subscriber) {
        this.subscribers.add(subscriber);
    }

    public void removeSubscriber(VkGroupSubscriberEntity subscriber) {
        this.subscribers.remove(subscriber);
    }
}
