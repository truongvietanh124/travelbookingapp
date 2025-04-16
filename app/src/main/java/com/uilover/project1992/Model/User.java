package com.uilover.project1992.Model;

public class User {
    private String email;
    private String name;
    private String phone;
    private String birthdate;
    private String hometown;
    private String paymentMethod;

    public User() {} // Bắt buộc với Firestore

    public User(String email, String name, String phone, String birthdate, String hometown, String paymentMethod) {
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.birthdate = birthdate;
        this.hometown = hometown;
        this.paymentMethod = paymentMethod;
    }

    // Getter & Setter nếu cần
}
