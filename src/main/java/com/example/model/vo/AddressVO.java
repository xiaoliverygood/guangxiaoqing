package com.example.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressVO {
    /**
     * 地址id
     */
    private String id;
    /**
     * 地址的省市区镇代码（从拿代码接口拿）
     */
    private String regionId;

    /**
     * 详细地址描述
     */
    private String detailedAddress;
}
