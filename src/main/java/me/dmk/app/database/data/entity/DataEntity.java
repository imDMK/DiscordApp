package me.dmk.app.database.data.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by DMK on 23.02.2023
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataEntity {
    String collection();
}
