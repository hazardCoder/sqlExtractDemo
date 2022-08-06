package com.qian.sqlextract.vo;

import lombok.*;

/**
 * @author Qian
 * @date 2022年08月06日 2:40
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Student {

    private Integer id;

    private String name;

    private Integer score;

    private Integer age;

    private Integer gender;

}
