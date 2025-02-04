package com.ruipeng.cloudstorage.entity;


import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;

    @Component
    @Data
    public class User {
        private Long id;
        private String email;
        private String passwordHash;
        private String name;
        private Instant createdAt;
        private Instant updatedAt;


        public User(Long id, String email, String passwordHash, String name, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.email = email;
            this.passwordHash = passwordHash;
            this.name = name;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public User() {
        }

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

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }


