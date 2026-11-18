package com.cityfix.dao;

import com.cityfix.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    int create(User user);
    Optional<User> findById(int userId);
    List<User> findAll();
}

