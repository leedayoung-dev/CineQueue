package com.cinequeue.backend.theater.repository;

import com.cinequeue.backend.theater.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

    List<Theater> findAllByOrderByNameAsc();
}
