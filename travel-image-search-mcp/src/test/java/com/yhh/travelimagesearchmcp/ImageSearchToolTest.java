package com.yhh.travelimagesearchmcp;

import com.yhh.travelimagesearchmcp.tools.ImageSearchTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImage() {
        String result = imageSearchTool.searchImage("computer");
        String[] urlArray = result.split(",");
        for (int i = 0; i < urlArray.length; i++) {
            String markdownUrl = "![](" + urlArray[i] + ")";
            System.out.println(markdownUrl);
        }
    }

}

