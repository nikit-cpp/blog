package com.github.nikit.cpp.controllers;

import com.github.nikit.cpp.config.CustomConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractImageUploadController {

    @Autowired
    private DataSource dataSource;

    @Value("${custom.image.max.bytes}")
    private long maxBytes;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractImageUploadController.class);

    @Autowired
    protected CustomConfig customConfig;

    public static final String IMAGE_PART = "image";


	// /post/123/title.png -> /post/123/title.png
	// /post/123/content/wqwqw.png -> /post/123/content/_timestamp_.png
	// /post/123/content/sasasas.png -> /post/123/content/_timestamp_.png
	// /user/123/kitty.png -> /user/123/avatar.png

    @FunctionalInterface
    public interface UpdateImage {
        void updateImage(Connection conn, long contentLength, String contentType);
    }


    public String putImage(
            MultipartFile imagePart,
            Consumer<Connection> insertOrNothing,
            UpdateImage updateImage,
            Function<MultipartFile, String> produceUrl
	) throws SQLException, IOException {
		long contentLength = getCorrectContentLength(imagePart.getSize());
        String contentType = imagePart.getContentType();
        MediaType.valueOf(contentType);

        try(Connection conn = dataSource.getConnection();) {
            insertOrNothing.accept(conn);
            updateImage.updateImage(conn, contentLength, contentType);
        }
        return produceUrl.apply(imagePart);
    }

    private long getCorrectContentLength(long contentLength) {
        if (contentLength > maxBytes) {
            throw new RuntimeException("Image must be <= "+ maxBytes + " bytes");
        }
        return contentLength;
    }

    protected String getExtension(String filename) {
        Assert.notNull(filename, "cannot be null");
        String[] parts = filename.split("\\.");
        return parts[parts.length-1];
    }

    public HttpHeaders getImage(
            Function<Connection, HttpHeaders> buildResponse
    ) throws SQLException, IOException {
        try(Connection conn = dataSource.getConnection();) {
            return buildResponse.apply(conn);
        }
    }

    protected HttpHeaders buildHeaders(ResultSet rs) throws SQLException {
        String contentType = getContentType(rs);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf(contentType));
        return httpHeaders;
    }

    protected abstract String getContentType(ResultSet rs) throws SQLException;

    protected void copy(InputStream from, OutputStream to) throws IOException {
		byte[] buffer = new byte[4 * 1024];
		int len;
		while ((len = from.read(buffer)) != -1) {
			to.write(buffer, 0, len);
		}
	}
}
 