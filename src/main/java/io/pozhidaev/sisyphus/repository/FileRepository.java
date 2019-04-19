package io.pozhidaev.sisyphus.repository;


import io.pozhidaev.sisyphus.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO make it abstract
 */
public interface FileRepository extends JpaRepository<File, Long> {


}
