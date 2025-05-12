package com.videochat.controller;

import com.videochat.dto.response.CallStatisticsResponse;
import com.videochat.model.CallHistory;
import com.videochat.model.CallStatus;
import com.videochat.model.User;
import com.videochat.service.CallHistoryService;
import com.videochat.service.UserService;
import com.videochat.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallHistoryController {

    private final CallHistoryService callHistoryService;
    private final UserService userService;
    private final WebSocketService webSocketService;

    @GetMapping("/history")
    public ResponseEntity<List<CallHistory>> getCallHistory(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(callHistoryService.getCallHistoryForUser(currentUser));
    }

    @PostMapping("/initiate/{userId}")
    public ResponseEntity<CallHistory> initiateCall(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId) {
        User receiver = userService.findById(userId);
        CallHistory callHistory = callHistoryService.createCallRecord(currentUser, receiver);

        // Notify the receiver about incoming call
        webSocketService.notifyIncomingCall(
                userId,
                callHistory.getId(),
                userService.getCurrentUser(currentUser)
        );

        return ResponseEntity.ok(callHistory);
    }

    @PutMapping("/{callId}/accept")
    public ResponseEntity<CallHistory> acceptCall(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long callId) {
        CallHistory callHistory = callHistoryService.updateCallStatus(callId, CallStatus.ANSWERED);

        // Notify the caller that call is accepted
        webSocketService.notifyCallAccepted(
                callHistory.getCaller().getId(),
                callId
        );

        return ResponseEntity.ok(callHistory);
    }

    @PutMapping("/{callId}/reject")
    public ResponseEntity<CallHistory> rejectCall(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long callId) {
        CallHistory callHistory = callHistoryService.updateCallStatus(callId, CallStatus.REJECTED);

        // Notify the caller that call is rejected
        webSocketService.notifyCallRejected(
                callHistory.getCaller().getId(),
                callId
        );

        return ResponseEntity.ok(callHistory);
    }

    @PutMapping("/{callId}/end")
    public ResponseEntity<CallHistory> endCall(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long callId) {
        CallHistory callHistory = callHistoryService.endCall(callId);

        // Notify the other party that call has ended
        Long otherUserId = currentUser.getId().equals(callHistory.getCaller().getId()) ?
                callHistory.getReceiver().getId() : callHistory.getCaller().getId();

        webSocketService.notifyCallEnded(otherUserId, callId);

        return ResponseEntity.ok(callHistory);
    }

    @GetMapping("/statistics")
    public ResponseEntity<CallStatisticsResponse> getCallStatistics(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        CallStatisticsResponse statistics = callHistoryService.getCallStatistics(currentUser, from, to);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/{callId}")
    public ResponseEntity<CallHistory> getCallById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long callId) {

        CallHistory callHistory = callHistoryService.getCallById(callId);

        // Verify the current user is part of this call
        if (!currentUser.getId().equals(callHistory.getCaller().getId()) &&
                !currentUser.getId().equals(callHistory.getReceiver().getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(callHistory);
    }
}