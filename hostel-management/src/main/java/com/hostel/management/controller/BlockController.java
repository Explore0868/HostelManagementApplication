package com.hostel.management.controller;

import com.hostel.management.dto.BlockResponse;
import com.hostel.management.service.BlockService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
@PreAuthorize("@securityUtils.isHostelOwner()")
public class BlockController {
    private final BlockService service;
    public BlockController(BlockService service) { this.service = service; }
    @GetMapping public List<BlockResponse> list() { return service.list(); }
    @PostMapping public BlockResponse create(@RequestBody Map<String, String> body) { return service.create(body.get("name")); }
    @PatchMapping public BlockResponse rename(@RequestBody Map<String, String> body) { return service.rename(body.get("currentName"), body.get("name")); }
    @DeleteMapping public void delete(@RequestParam String name) { service.delete(name); }
}
