package com.github.nikit.cpp.controllers;

import com.github.nikit.cpp.dto.UserAccountDetailsDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class ImagePostContentUploadController extends AbstractImageUploadController {

    public static final String POST_CONTENT_IMAGE_URL_TEMPLATE = "/api/post/{post_id}/content";
    public static final String POST_CONTENT_IMAGE_URL_TEMPLATE_WITH_FILENAME = POST_CONTENT_IMAGE_URL_TEMPLATE + "/{id}.{ext}";

    @PostMapping(POST_CONTENT_IMAGE_URL_TEMPLATE)
    @PreAuthorize("@blogSecurityService.hasPostContentImagePermission(#postId, #userAccount, T(com.github.nikit.cpp.entity.jpa.Permissions).EDIT)")
    public String putImage(
            @RequestPart(value = IMAGE_PART) MultipartFile imagePart,
            @PathVariable("post_id")long postId,
            @NotNull @AuthenticationPrincipal UserAccountDetailsDTO userAccount
    ) throws SQLException, IOException {
        AtomicLong idWr = new AtomicLong();
        return super.putImage(
            imagePart,
            (Connection conn) -> {
                try(PreparedStatement psI = conn.prepareStatement("INSERT INTO images.post_content_image(post_id, id, img, content_type) VALUES (?, DEFAULT, NULL, NULL) RETURNING id;");) {
                    psI.setLong(1, postId);
                    try(ResultSet ids = psI.executeQuery()) {
                        if(ids.next()) {
                            idWr.set(ids.getLong("id"));
                        } else {
                            throw new RuntimeException("id did not returned");
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            (conn, contentLength, contentType) -> {
                try(PreparedStatement psU = conn.prepareStatement("UPDATE images.post_content_image SET img = ?, content_type = ? WHERE id = ? and post_id = ?");) {
                    psU.setLong(4, postId);
                    psU.setLong(3, idWr.get());
                    psU.setString(2, contentType);
                    psU.setBinaryStream(1, imagePart.getInputStream(), (int) contentLength);
                    psU.executeUpdate();
                } catch (IOException | SQLException e) {
                    throw new RuntimeException(e);
                }
            },
            multipartFile -> UriComponentsBuilder.fromUriString(customConfig.getBaseUrl() + POST_CONTENT_IMAGE_URL_TEMPLATE_WITH_FILENAME)
                    .buildAndExpand(postId, idWr.get(), getExtension(imagePart.getOriginalFilename()))
                    .toUriString()
        );
    }

    private static final String ID = "id";
    private static final String POST_ID = "post_id";

    private Map<String, Object> toComplexId(long id, long postId) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, id);
        map.put(POST_ID, postId);
        return map;
    }

    private long toLongId(Map<String, Object> id) {
        return (long) id.get(ID);
    }

    private long toLongPostId(Map<String, Object> id) {
        return (long) id.get(POST_ID);
    }


    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////

    @GetMapping(POST_CONTENT_IMAGE_URL_TEMPLATE_WITH_FILENAME)
    public HttpHeaders getImage(
            @PathVariable("post_id")long postId,
            @PathVariable("id")long id,
            OutputStream response
    ) throws SQLException, IOException {
        return super.getImage(
                (Connection conn) -> {
                    try (PreparedStatement ps = conn.prepareStatement("SELECT img, content_type FROM images.post_content_image WHERE id = ? AND post_id = ?");) {
                        ps.setLong(1, id);
                        ps.setLong(2, postId);
                        try (ResultSet rs = ps.executeQuery();) {
                            if (rs.next()) {
                                try(InputStream imgStream = rs.getBinaryStream("img");){
                                    copy(imgStream, response);
                                } catch (SQLException | IOException e) {
                                    throw new RuntimeException(e);
                                }
                                return buildHeaders(rs);
                            } else {
                                throw new RuntimeException("post content image with id '"+id+"' not found");
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Override
    protected String getContentType(ResultSet rs) throws SQLException {
        return rs.getString("content_type");
    }
}
 