package com.carlos.mongodb.tool;

import com.mongoplus.conditions.query.QueryChainWrapper;
import com.mongoplus.conditions.query.QueryWrapper;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * <p>
 * MongoDB 查询构建器 — 字符串字段名方式的动态查询构建
 * </p>
 *
 * <p><strong>与 mongo-plus 的关系：</strong>mongo-plus 的
 * {@link QueryChainWrapper} / {@link QueryWrapper} 推荐用于 Lambda 类型安全查询，
 * 本构建器适用于字段名动态传入的场景（如前端传入排序字段名）。</p>
 *
 * @author carlos
 * @date 2024/01/15
 */
public class MongoQueryBuilder {

    private final Query query;
    private final Criteria criteria;

    public MongoQueryBuilder() {
        this.criteria = new Criteria();
        this.query = new Query(this.criteria);
    }

    public static MongoQueryBuilder create() {
        return new MongoQueryBuilder();
    }

    public MongoQueryBuilder eq(String field, Object value) {
        if (value != null) {
            this.query.addCriteria(Criteria.where(field).is(value));
        }
        return this;
    }

    public MongoQueryBuilder ne(String field, Object value) {
        if (value != null) {
            this.query.addCriteria(Criteria.where(field).ne(value));
        }
        return this;
    }

    public MongoQueryBuilder gt(String field, Object value) {
        if (value != null) {
            this.query.addCriteria(Criteria.where(field).gt(value));
        }
        return this;
    }

    public MongoQueryBuilder gte(String field, Object value) {
        if (value != null) {
            this.query.addCriteria(Criteria.where(field).gte(value));
        }
        return this;
    }

    public MongoQueryBuilder lt(String field, Object value) {
        if (value != null) {
            this.query.addCriteria(Criteria.where(field).lt(value));
        }
        return this;
    }

    public MongoQueryBuilder lte(String field, Object value) {
        if (value != null) {
            this.query.addCriteria(Criteria.where(field).lte(value));
        }
        return this;
    }

    public MongoQueryBuilder between(String field, Object start, Object end) {
        if (start != null && end != null) {
            this.query.addCriteria(Criteria.where(field).gte(start).lte(end));
        } else if (start != null) {
            gte(field, start);
        } else if (end != null) {
            lte(field, end);
        }
        return this;
    }

    public MongoQueryBuilder like(String field, String value) {
        if (value != null && !value.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).regex(".*" + Pattern.quote(value) + ".*", "i"));
        }
        return this;
    }

    public MongoQueryBuilder likeLeft(String field, String value) {
        if (value != null && !value.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).regex(".*" + Pattern.quote(value) + "$", "i"));
        }
        return this;
    }

    public MongoQueryBuilder likeRight(String field, String value) {
        if (value != null && !value.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).regex("^" + Pattern.quote(value) + ".*", "i"));
        }
        return this;
    }

    public MongoQueryBuilder regex(String field, String regex) {
        if (regex != null && !regex.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).regex(regex));
        }
        return this;
    }

    public MongoQueryBuilder regex(String field, String regex, String options) {
        if (regex != null && !regex.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).regex(regex, options));
        }
        return this;
    }

    public MongoQueryBuilder in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).in(values));
        }
        return this;
    }

    public MongoQueryBuilder in(String field, Object... values) {
        if (values != null && values.length > 0) {
            this.query.addCriteria(Criteria.where(field).in(values));
        }
        return this;
    }

    public MongoQueryBuilder nin(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            this.query.addCriteria(Criteria.where(field).nin(values));
        }
        return this;
    }

    public MongoQueryBuilder isNull(String field) {
        this.query.addCriteria(Criteria.where(field).isNull());
        return this;
    }

    public MongoQueryBuilder isNotNull(String field) {
        this.query.addCriteria(Criteria.where(field).ne(null));
        return this;
    }

    public MongoQueryBuilder exists(String field, boolean exists) {
        this.query.addCriteria(Criteria.where(field).exists(exists));
        return this;
    }

    public MongoQueryBuilder addCriteria(Criteria criteria) {
        this.query.addCriteria(criteria);
        return this;
    }

    public MongoQueryBuilder or(Criteria... criterias) {
        this.query.addCriteria(new Criteria().orOperator(criterias));
        return this;
    }

    public MongoQueryBuilder and(Criteria... criterias) {
        this.query.addCriteria(new Criteria().andOperator(criterias));
        return this;
    }

    /**
     * 构建 Spring Data Query 对象
     *
     * @return Query
     */
    public Query build() {
        return this.query;
    }
}
