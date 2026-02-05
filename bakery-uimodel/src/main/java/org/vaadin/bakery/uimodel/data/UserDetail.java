package org.vaadin.bakery.uimodel.data;

import org.vaadin.bakery.uimodel.type.UserRole;

/**
 * UI model for user create/edit form.
 */
public class UserDetail {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private UserRole role;
    private byte[] photo;
    private String photoContentType;
    private Long primaryLocationId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
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

    public Long getPrimaryLocationId() {
        return primaryLocationId;
    }

    public void setPrimaryLocationId(Long primaryLocationId) {
        this.primaryLocationId = primaryLocationId;
    }

    public boolean isNew() {
        return id == null;
    }
}
