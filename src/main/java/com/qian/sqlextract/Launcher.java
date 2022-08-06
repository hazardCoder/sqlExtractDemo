package com.qian.sqlextract;

import com.mysql.jdbc.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Qian
 * @date 2022年08月06日 2:44
 */
public class Launcher {

    public static void main(String[] args) throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        Configuration configuration = sqlSessionFactory.getConfiguration();
        Collection<MappedStatement> mappedStatements = configuration.getMappedStatements();
        for (MappedStatement mappedStatement : mappedStatements) {
            System.out.println(getSql(mappedStatement));
        }
    }

    public static String getSql(MappedStatement statement) {
        SqlSource sqlSource = statement.getSqlSource();
        if (sqlSource instanceof DynamicSqlSource) {
            return getSqlInDyna((DynamicSqlSource) sqlSource);
        } else {
            return sqlSource.getBoundSql(null).getSql();
        }
    }

    public static String getSqlInDyna(DynamicSqlSource sqlSource) {
        try {
            Field sqlNodeField = sqlSource.getClass().getDeclaredField("rootSqlNode");
            sqlNodeField.setAccessible(true);
            SqlNode sqlNode = (SqlNode) sqlNodeField.get(sqlSource);
            return getSqlInSqlNode(sqlNode);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getSqlInSqlNode(SqlNode sqlNode) throws NoSuchFieldException, IllegalAccessException {
        if (sqlNode instanceof MixedSqlNode) {
            return getSqlInMixedSqlNode((MixedSqlNode) sqlNode);
        } else if (sqlNode instanceof IfSqlNode) {
            return getSqlInIfSqlNode((IfSqlNode) sqlNode);
        } else if (sqlNode instanceof StaticTextSqlNode) {
            return getSqlInStaticSqlNode((StaticTextSqlNode) sqlNode);
        }
        return "";
    }

    public static String getSqlInStaticSqlNode(StaticTextSqlNode sqlNode) throws NoSuchFieldException, IllegalAccessException {
        Field text = sqlNode.getClass().getDeclaredField("text");
        text.setAccessible(true);
        return text.get(sqlNode).toString().trim();
    }

    public static String getSqlInMixedSqlNode(MixedSqlNode sqlNode) throws NoSuchFieldException, IllegalAccessException {
        Field contentsField = sqlNode.getClass().getDeclaredField("contents");
        contentsField.setAccessible(true);
        List<SqlNode> sqlNodeList = (List<SqlNode>) contentsField.get(sqlNode);
        StringJoiner sql = new StringJoiner(" ");
        for (SqlNode node : sqlNodeList) {
            sql.add(getSqlInSqlNode(node));
        }
        return sql.toString();
    }

    public static String getSqlInIfSqlNode(IfSqlNode node) throws NoSuchFieldException, IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        Class<? extends IfSqlNode> clazz = node.getClass();
        Field testField = clazz.getDeclaredField("test");
        testField.setAccessible(true);
        String test = (String) testField.get(node);
        // if
        if (!StringUtils.isNullOrEmpty(test)) {
            sb.append("<");
            sb.append(test);
            sb.append(">");
        }
        // if true
        Field contentsField = clazz.getDeclaredField("contents");
        contentsField.setAccessible(true);
        SqlNode sqlNode = (SqlNode) contentsField.get(node);
        sb.append("[");
        sb.append(getSqlInSqlNode(sqlNode));
        sb.append("]");
        return sb.toString();
    }
}
