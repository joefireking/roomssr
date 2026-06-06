package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.AnnouncementDTO;
import com.apartment.hub.entity.Announcement;
import com.apartment.hub.security.LoginUser;
import com.apartment.hub.service.AnnouncementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/list")
    public Result<PageResult<Announcement>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Announcement::getStatus, status)
                .orderByDesc(Announcement::getTopFlag)
                .orderByDesc(Announcement::getCreateTime);
        return Result.success(PageResult.from(announcementService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/latest")
    public Result<List<Announcement>> latest(@RequestParam(defaultValue = "5") int limit) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getTopFlag)
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT " + Math.min(limit, 20));
        return Result.success(announcementService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Announcement> getById(@PathVariable Long id) {
        return Result.success(announcementService.getById(id));
    }

    @OperationLog(module = "Announcement Management", operation = "Create Announcement")
    @PostMapping
    public Result<Announcement> create(@Valid @RequestBody AnnouncementDTO dto,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setSummary(dto.getSummary());
        announcement.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        announcement.setTopFlag(dto.getTopFlag() != null ? dto.getTopFlag() : 0);
        announcement.setPublisherId(loginUser.getUserId());
        announcementService.save(announcement);
        return Result.success(announcement);
    }

    @OperationLog(module = "Announcement Management", operation = "Update Announcement")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody AnnouncementDTO dto) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) return Result.fail("Announcement not found");
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setSummary(dto.getSummary());
        if (dto.getStatus() != null) announcement.setStatus(dto.getStatus());
        if (dto.getTopFlag() != null) announcement.setTopFlag(dto.getTopFlag());
        return Result.success(announcementService.updateById(announcement));
    }

    @OperationLog(module = "Announcement Management", operation = "Delete Announcement")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(announcementService.removeById(id));
    }
}
