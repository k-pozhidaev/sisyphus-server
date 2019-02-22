package io.pozhidaev.sisyphus.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class File {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(name = "MIME_TYPE")
    private String mimeType;
    @Column(name = "CONTENT_LENGTH")
    private Long contentLength;
    @Column(name = "CONTENT_OFFSET")
    private Long contentOffset;
    @Column(name = "LAST_UPLOADED_CHUNK_NUMBER")
    private Long lastUploadedChunkNumber;
    @Column(name = "ORIGINAL_NAME")
    private String originalName;
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onPersist(){
        createdAt = LocalDateTime.now(ZoneId.systemDefault());
        updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
