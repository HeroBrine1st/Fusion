package ru.herobrine1st.fusion.module.vk.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

import javax.annotation.Nullable;
import java.util.List;

@Entity
@Table(name = "vk_groups")
public class VkGroupEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @NaturalId
    @Column(nullable = false)
    private long groupId;

    @Column(nullable = false)
    private String name;

    @Column
    private String avatarUrl;

    @Column(nullable = false)
    private long lastWallPostId; // To avoid duplicates

    @Column(nullable = false)
    private String originalLink;

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

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(@Nullable String avatarUrl) {
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

    public void addSubscriber(VkGroupSubscriberEntity subscriber) {
        this.subscribers.add(subscriber);
        subscriber.setGroup(this);
    }

    public void removeSubscriber(VkGroupSubscriberEntity subscriber) {
        this.subscribers.remove(subscriber);
        subscriber.setGroup(null);
    }

    public String getOriginalLink() {
        return originalLink;
    }

    public void setOriginalLink(String originalLink) {
        this.originalLink = originalLink;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
}
