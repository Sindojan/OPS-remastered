package com.sindoflow.ops.inbox;

import com.sindoflow.ops.common.ApiResponse;
import com.sindoflow.ops.inbox.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService,
                                  MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> list(
            Pageable pageable,
            @RequestParam(value = "status", required = false) String status) {

        Page<ConversationResponse> page = conversationService.findAll(pageable, status)
                .map(ConversationResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getById(@PathVariable UUID id) {
        ConversationEntity conversation = conversationService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(ConversationResponse.from(conversation)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> create(
            @Valid @RequestBody ConversationCreateRequest request) {

        ConversationEntity conversation = conversationService.create(
                request.subject(), request.customerId(), request.priority(), request.source());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ConversationResponse.from(conversation), "Conversation created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ConversationUpdateRequest request) {

        ConversationEntity conversation = conversationService.update(
                id, request.subject(), request.customerId(), request.priority());
        return ResponseEntity.ok(ApiResponse.ok(ConversationResponse.from(conversation)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ConversationResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {

        ConversationEntity conversation = conversationService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(ConversationResponse.from(conversation)));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<ConversationResponse>> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRequest request) {

        ConversationEntity conversation = conversationService.assign(id, request.assignedTo());
        return ResponseEntity.ok(ApiResponse.ok(ConversationResponse.from(conversation)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody MessageCreateRequest request) {

        ConversationMessageEntity message = messageService.addMessage(
                id, request.content(), request.senderType(), request.senderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(MessageResponse.from(message), "Message added"));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable UUID id,
            Pageable pageable) {

        Page<MessageResponse> page = messageService.getMessages(id, pageable)
                .map(MessageResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<Void>> addTag(
            @PathVariable UUID id,
            @Valid @RequestBody TagRequest request) {

        conversationService.addTag(id, request.tag());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "Tag added"));
    }

    @DeleteMapping("/{id}/tags/{tag}")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @PathVariable UUID id,
            @PathVariable String tag) {

        conversationService.removeTag(id, tag);
        return ResponseEntity.ok(ApiResponse.ok(null, "Tag removed"));
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<ApiResponse<ConversationLinkResponse>> linkEntity(
            @PathVariable UUID id,
            @Valid @RequestBody ConversationLinkRequest request) {

        ConversationLinkEntity link = conversationService.linkEntity(id, request.linkedType(), request.linkedId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ConversationLinkResponse.from(link), "Link created"));
    }
}
