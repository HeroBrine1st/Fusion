package ru.herobrine1st.fusion.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "vk_group_subscribers")
public class VkGroupSubscriberEntity {
    public VkGroupSubscriberEntity() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private long id;

    @Column(nullable = false)
    private long guildId;

    @Column(nullable = false)
    private long channelId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private VkGroupEntity group;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    public VkGroupEntity getGroup() {
        return group;
    }

    public void setGroup(VkGroupEntity group) {
        this.group = group;
    }
}
