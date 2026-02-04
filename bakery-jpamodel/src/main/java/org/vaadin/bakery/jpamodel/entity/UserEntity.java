package org.vaadin.bakery.jpamodel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.vaadin.bakery.jpamodel.code.UserRoleCode;
import org.vaadin.bakery.jpamodel.projection.UserSummaryProjection;

/**
 * Staff members who can log into the system.
 */
@Entity
@Table(name = "app_user")
public class UserEntity extends AbstractAuditableEntity implements UserSummaryProjection {

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRoleCode role;

    @Lob
    @Column(name = "photo", columnDefinition = "BLOB")
    private byte[] photo;

    @Column(name = "photo_content_type")
    private String photoContentType;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRoleCode getRole() {
        return role;
    }

    public void setRole(UserRoleCode role) {
        this.role = role;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }
}
