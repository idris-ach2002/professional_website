package sorbonne.professional_website.visibility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "front_item_visibility")
public class FrontItemVisibility {

    @Id
    @Column(name = "item_key", nullable = false, length = 180)
    private String itemKey;

    @Column(nullable = false)
    private boolean visible = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public FrontItemVisibility() {
    }

    public FrontItemVisibility(String itemKey, boolean visible) {
        this.itemKey = itemKey;
        this.visible = visible;
    }

    public String getItemKey() {
        return itemKey;
    }

    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
