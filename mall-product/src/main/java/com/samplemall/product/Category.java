package com.samplemall.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Category {

    /** 稳定编码，前端筛选参数直接使用（如 green / oolong）。 */
    @Id
    @Column(length = 32)
    private String code;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false)
    private int sort;

    protected Category() {
    }

    public Category(String code, String name, int sort) {
        this.code = code;
        this.name = name;
        this.sort = sort;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getSort() {
        return sort;
    }
}
