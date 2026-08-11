package com.stockresearch.repository;

import com.stockresearch.domain.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    List<News> findByCompanyIdOrderByPublishedAtDesc(Long companyId);

    List<News> findAllByOrderByPublishedAtDesc(Pageable pageable);

    List<News> findByMatchesGovernmentThemeTrueOrderByPublishedAtDesc(Pageable pageable);
}
