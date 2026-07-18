package com.wireblog.controller;

import com.wireblog.dto.ShareRequest;
import com.wireblog.service.ShareService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    public void share(@PathVariable Long postId, @Valid @RequestBody ShareRequest req) {
        shareService.share(postId, req);
    }
}
