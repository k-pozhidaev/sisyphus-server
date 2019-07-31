package io.pozhidaev.sisyphus.repository;


import io.pozhidaev.sisyphus.domain.File;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface FileRepository extends PagingAndSortingRepository<File, Long> {


}
