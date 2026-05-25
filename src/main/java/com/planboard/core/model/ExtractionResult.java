package com.planboard.core.model;

/**
 * 提取结果 — 核心纯 Java，无任何 Minecraft/mod 依赖
 */
public final class ExtractionResult {

    public enum Status {
        SUCCESS,
        INVENTORY_FULL,
        NOTHING_TO_DO,
        EMPTY_PLAN,
        NO_CONTAINER_ACCESS
    }

    private final Status status;
    private final int extractedTypes;
    private final int totalItems;
    private final String message;

    private ExtractionResult(Status status, int extractedTypes, int totalItems, String message) {
        this.status = status;
        this.extractedTypes = extractedTypes;
        this.totalItems = totalItems;
        this.message = message;
    }

    public static ExtractionResult success(int extractedTypes, int totalItems) {
        return new ExtractionResult(
                Status.SUCCESS,
                extractedTypes,
                totalItems,
                "Extracted " + totalItems + " items (" + extractedTypes + " types)"
        );
    }

    public static ExtractionResult inventoryFull(int extractedTypes, int totalItems) {
        return new ExtractionResult(
                Status.INVENTORY_FULL,
                extractedTypes,
                totalItems,
                "Inventory full! Extracted " + totalItems + " items"
        );
    }

    public static ExtractionResult nothingToDo() {
        return new ExtractionResult(Status.NOTHING_TO_DO, 0, 0, "All items already satisfied");
    }

    public static ExtractionResult emptyPlan() {
        return new ExtractionResult(Status.EMPTY_PLAN, 0, 0, "Plan board is empty");
    }

    public static ExtractionResult noAccess() {
        return new ExtractionResult(Status.NO_CONTAINER_ACCESS, 0, 0, "Cannot access container");
    }

    public Status getStatus() {
        return status;
    }

    public int getExtractedTypes() {
        return extractedTypes;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isInventoryFull() {
        return status == Status.INVENTORY_FULL;
    }
}
