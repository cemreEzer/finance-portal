package com.finance.dto;

import java.time.LocalDateTime;

/**
 * Standart API yanıt sarmalayıcısı.
 * Doküman isterlerine uygun: tutarlı response formatı.
 *
 * @param <T> Yanıt verisi tipi
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private int count;
    private LocalDateTime timestamp;

    /* ─── Static Factory Methods ──────────────────────────────── */

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = "İşlem başarılı";
        response.data = data;
        response.count = 1;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> success(T data, int count) {
        ApiResponse<T> response = success(data);
        response.count = count;
        return response;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = success(data);
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.data = null;
        response.count = 0;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    /* ─── Getters & Setters ───────────────────────────────────── */

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
