package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.datamagic.bridge.ConditionType;
import com.ninestar.datapie.datamagic.bridge.TableListReqType;
import com.ninestar.datapie.datamagic.consts.BoolOperator;
import com.ninestar.datapie.datamagic.consts.MatchType;
import com.ninestar.datapie.datamagic.entity.SysOrgEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import javax.persistence.metamodel.Bindable;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * @author Gavin.Zhao
 * date 2021-10-14
 * util tool for Jpa specification
 */

/*
refer to https://gitee.com/zhouacai/specifcation-utils/tree/master
JpaSpecUtil.<UserDO>and()
				// 直接使用的方式会判断后面的参数是否为空，如果为空就不会起作用
				// 如果第二个参数为空，则不会生效
				.equal("userName", userVO.getUserName())
				.like("name", userVO.getName())
				// 实体类中的类型为Instant 就使用betweenInstant，为Date就使用betweenDate 参数为List<String>  日期格式为yyyy-MM-dd
				.betweenInstant("createdDate", userVO.getCreatedDate())
				// 相当于or里面的内容加了个括号
				.and(Specifications.<UserDO>or()
						.equal("phone", userVO.getPhone())
						.equal("email", "email不相等")
						.build())
				// 如果没有找到与你期望的预设方法，可以使用下面的方法自定义条件
				.and((root, query, builder) -> builder.greaterThan(root.get("version"), 0))
				// 直接修改后面的方法为or 不太实用，最好还是使用or(Specification spec)
//				.or()
//				.like("avatar", "这里是肯定不等于值的，只是做个测试")
				.build();
*/

//https://blog.csdn.net/a627428179/article/details/100026568
//https://azhengzj.gitee.io/zuji-jpa/#/, 类似于mybatis-plus的条件构造器
//https://www.cnblogs.com/wangjing666/p/7383121.html

// how to merge the two classes and keep it as a static utility?
// To do -- improve it's usage for this project
//usage: JpaSpecUtil.<SysUserEntity>and().equal("Gavin", user.getName()).like("Jichun", user.getNickname()).done();
//usage: JpaSpecUtil.<SysUserEntity>build(BoolOperator.AND).equal("Gavin", user.getName()).like("Jichun", user.getNickname()).done();
//usage: JpaSpecUtil.build(tokenOrgId,false,req.filter, req.search);
public class JpaSpecUtil {

    public static <T> JpaSpecImpl<T> and() {
        return new JpaSpecImpl<>(BoolOperator.AND);
    }

    public static <T> JpaSpecImpl<T> or() {
        return new JpaSpecImpl<>(BoolOperator.OR);
    }

    public static  <T> Specification<T> build(Integer orgId, Boolean isSuperuser, TableListReqType.FilterType filter, TableListReqType.SearchType search) {
        return new JpaSpecImpl().buildSpecification(orgId,isSuperuser,filter, search);
    }

    public static  <T> Specification<T> build(Integer orgId, Integer userId, TableListReqType.FilterType filter, TableListReqType.SearchType search) {
        return new JpaSpecImpl().buildSpecification(orgId, userId, filter, search);
    }

    public static  <T> JpaSpecImpl<T> build(BoolOperator operator) {
        if(operator.equals(BoolOperator.AND)){
            return new JpaSpecImpl<>(BoolOperator.AND);
        }
        else if(operator.equals(BoolOperator.OR)){
            return new JpaSpecImpl<>(BoolOperator.AND);
        }
        else{
            return null;
        }
    }

    public static  <T> Specification<T> build(BoolOperator operator, List<ConditionType> conditions) {
        // generate spec based on conditions like buildSpecification()
        // to do --
        return null;
    }

    public static class JpaSpecImpl<T> {

        private Specification<T> spec;

        private Method specMethod;

        public Specification<T> buildSpecification(Integer orgId, Boolean isSuperuser, TableListReqType.FilterType filter, TableListReqType.SearchType search) {
            // build query conditions
            Specification<T> specification = new Specification<T>() {
                @Override
                public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    List<Predicate> preconditions = new ArrayList<>();
                    List<Predicate> filterConditions = new ArrayList<>();
                    List<Predicate> searchConditions = new ArrayList<>();
                    Predicate prePredicate = null;
                    Predicate filterPredicate = null;
                    Predicate searchPredicate = null;
                    Predicate finalPredicate = null;

                    if (!isSuperuser && orgId!=null) {
                        // only superuser can see users of all orgs
                        preconditions.add(cb.equal(root.get("org"), orgId));
                        prePredicate = cb.and(preconditions.toArray(new Predicate[preconditions.size()]));
                    }

                    // build filter query condition and predicate
                    if (filter != null && filter.fields !=null && filter.fields.size() > 0 && filter.values.size() > 0) {
                        // only one filter for now
                        for (int i = 0; i < filter.fields.size(); i++) {
                            String fieldName = filter.fields.get(i);
                            String[] fieldValue = filter.values.get(i);
                            // convert string to boolean if needed
                            if (fieldValue[0].equalsIgnoreCase("true") || fieldValue[0].equalsIgnoreCase("false")) {
                                Boolean[] booleans = new Boolean[fieldValue.length];
                                for (int j = 0; j < fieldValue.length; j++) {
                                    booleans[j] = fieldValue[j].equalsIgnoreCase("true") ? true : false;
                                }
                                Path<T> ccc = root.get(fieldName);
                                filterConditions.add(root.get(fieldName).in(booleans));
                            }
                            else if(root.get(fieldName).getJavaType().getName().startsWith("java.")){
                                // regular type like Integer, string
                                filterConditions.add(root.get(fieldName).in(fieldValue[i]));
                            } else {
                                // class like SysOrgEntity
                                // use fixed field 'id' here, need enhancement, Gavin !!!
                                filterConditions.add(root.get(fieldName).get("id").in(fieldValue[i]));
                            }
                        }
                        // 'or' for filtering multiple values in one field(column)
                        filterPredicate = cb.or(filterConditions.toArray(new Predicate[filterConditions.size()]));
                    }

                    // build search query condition and predicate
                    if (search != null && search.fields!=null && !StrUtil.isEmpty(search.value)) {
                        for (int k = 0; k < search.fields.length; k++) {
                            // fuzzy search
                            searchConditions.add(cb.like(root.get(search.fields[k]), "%" + search.value + "%"));
                        }
                        // 'or' for searching a value in multiple fields(columns)
                        searchPredicate = cb.or(searchConditions.toArray(new Predicate[searchConditions.size()]));
                    }

                    // return predicate
                    if (filterPredicate != null && searchPredicate != null) {
                        // 'and' is used between filter and search
                        finalPredicate = cb.and(filterPredicate, searchPredicate);
                    } else if (filterPredicate != null) {
                        // filter only
                        finalPredicate = filterPredicate;
                    } else if (searchPredicate != null) {
                        // search only
                        finalPredicate = searchPredicate;
                    }

                    if (finalPredicate != null && prePredicate != null) {
                        // prePredicate will be added with 'and' if not superuser
                        return cb.and(finalPredicate, prePredicate);
                    } else if (finalPredicate != null) {
                        // superuser with filter or search
                        return finalPredicate;
                    } else if (prePredicate != null) {
                        // normal user without any filter/search
                        return prePredicate;
                    } else {
                        // superuser without any predicate
                        return null;
                    }
                }
            };
            return specification;
        }


        public Specification<T> buildSpecification(Integer orgId, Integer userId, TableListReqType.FilterType filter, TableListReqType.SearchType search) {
            // build query conditions
            Specification<T> specification = new Specification<T>() {
                @Override
                public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    List<Predicate> preconditions = new ArrayList<>();
                    List<Predicate> filterConditions = new ArrayList<>();
                    List<Predicate> searchConditions = new ArrayList<>();
                    Predicate prePredicate = null;
                    Predicate filterPredicate = null;
                    Predicate searchPredicate = null;
                    Predicate finalPredicate = null;

                    if (orgId!=null) {
                        // filter by orgId
                        preconditions.add(cb.equal(root.get("org"), orgId));
                        prePredicate = cb.and(preconditions.toArray(new Predicate[preconditions.size()]));

                        // how to build a 'where orgId=? and (userId=? or pubFlag=true)'
                        // Gavin!!!
                    }

                    // build filter query condition and predicate
                    if (filter != null && filter.fields !=null && filter.fields.size() > 0 && filter.values.size() > 0) {
                        // only one filter for now
                        for (int i = 0; i < filter.fields.size(); i++) {
                            String fieldName = filter.fields.get(i);
                            String[] fieldValue = filter.values.get(i);
                            // convert string to boolean if needed
                            if (fieldValue[0].equalsIgnoreCase("true") || fieldValue[0].equalsIgnoreCase("false")) {
                                Boolean[] booleans = new Boolean[fieldValue.length];
                                for (int j = 0; j < fieldValue.length; j++) {
                                    booleans[j] = fieldValue[j].equalsIgnoreCase("true") ? true : false;
                                }
                                Path<T> ccc = root.get(fieldName);
                                filterConditions.add(root.get(fieldName).in(booleans));
                            }
                            else if(root.get(fieldName).getJavaType().getName().startsWith("java.")){
                                // regular type like Integer, string
                                filterConditions.add(root.get(fieldName).in(fieldValue[i]));
                            } else {
                                // class like SysOrgEntity
                                // use fixed field 'id' here, need enhancement, Gavin !!!
                                filterConditions.add(root.get(fieldName).get("id").in(fieldValue[i]));
                            }
                        }
                        // 'or' for filtering multiple values in one field(column)
                        filterPredicate = cb.or(filterConditions.toArray(new Predicate[filterConditions.size()]));
                    }

                    // build search query condition and predicate
                    if (search != null && search.fields!=null && !StrUtil.isEmpty(search.value)) {
                        for (int k = 0; k < search.fields.length; k++) {
                            // fuzzy search
                            searchConditions.add(cb.like(root.get(search.fields[k]), "%" + search.value + "%"));
                        }
                        // 'or' for searching a value in multiple fields(columns)
                        searchPredicate = cb.or(searchConditions.toArray(new Predicate[searchConditions.size()]));
                    }

                    // return predicate
                    if (filterPredicate != null && searchPredicate != null) {
                        // 'and' is used between filter and search
                        finalPredicate = cb.and(filterPredicate, searchPredicate);
                    } else if (filterPredicate != null) {
                        // filter only
                        finalPredicate = filterPredicate;
                    } else if (searchPredicate != null) {
                        // search only
                        finalPredicate = searchPredicate;
                    }

                    if (finalPredicate != null && prePredicate != null) {
                        // prePredicate will be added with 'and' if not superuser
                        return cb.and(finalPredicate, prePredicate);
                    } else if (finalPredicate != null) {
                        // superuser with filter or search
                        return finalPredicate;
                    } else if (prePredicate != null) {
                        // normal user without any filter/search
                        return prePredicate;
                    } else {
                        // superuser without any predicate
                        return null;
                    }
                }
            };
            return specification;
        }



        public JpaSpecImpl() {
        }


        public JpaSpecImpl(BoolOperator operator) {
            try {
                this.spec = Specification.where(null);
                this.specMethod = this.spec.getClass().getMethod(operator.getCode(), Specification.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        /**
         * 执行and 或者or方法
         *
         * @param specification specification
         */
        private void invoke(Specification<T> specification) {
            try {
                this.spec = (Specification<T>) this.specMethod.invoke(this.spec, specification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 去重
         *
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> distinct() {
            Specification<T> specification = (root, query, builder) -> {
                query.distinct(true);
                return null;
            };
            this.invoke(specification);
            return this;
        }

        /**
         * like
         *
         * @param key   字段名
         * @param value 字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> like(String key, String value) {
            if (this.check(MatchType.LIKE, new Object[]{value})) {
                Specification<T> specification = (root, query, builder) -> builder.like(root.get(key), this.getLikePattern(value));
                this.invoke(specification);
            }
            return this;
        }

        /**
         * like
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> like(String outer, String inside, String value) {
            if (this.check(MatchType.LIKE, new Object[]{value})) {
                Specification<T> specification = (root, query, builder) -> builder.like(root.get(outer).get(inside), this.getLikePattern(value));
                this.invoke(specification);
            }
            return this;
        }

        /**
         * like
         *
         * @param key1  字段
         * @param key2  字段
         * @param key3  字段
         * @param value 字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> like(String key1, String key2, String key3, String value) {
            if (this.check(MatchType.LIKE, new Object[]{value})) {
                Specification<T> specification = (root, query, builder) -> builder.like(root.get(key1).get(key2).get(key3), this.getLikePattern(value));
                this.invoke(specification);
            }
            return this;
        }

        /**
         * not like
         *
         * @param key   字段名
         * @param value 字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notLike(String key, String value) {
            if (this.check(MatchType.LIKE, new Object[]{value})) {
                Specification<T> specification = (root, query, builder) -> builder.like(root.get(key), this.getLikePattern(value)).not();
                this.invoke(specification);
            }
            return this;
        }

        /**
         * not like
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notLike(String outer, String inside, String value) {
            if (this.check(MatchType.LIKE, new Object[]{value})) {
                Specification<T> specification = (root, query, builder) -> builder.like(root.get(outer).get(inside), this.getLikePattern(value)).not();
                this.invoke(specification);
            }
            return this;
        }

        /**
         * equal
         *
         * @param key   字段名
         * @param value 字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> equal(String key, Object value) {
            if (this.check(MatchType.EQUAL, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.equal(root.get(key), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * equal
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> equal(String outer, String inside, Object value) {
            if (this.check(MatchType.EQUAL, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.equal(root.get(outer).get(inside), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * not equal
         *
         * @param key   字段名
         * @param value 字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notEqual(String key, Object value) {
            if (this.check(MatchType.EQUAL, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.equal(root.get(key), value).not();
                this.invoke(specification);
            }
            return this;
        }

        /**
         * not equal
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  字段值
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notEqual(String outer, String inside, Object value) {
            if (this.check(MatchType.EQUAL, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.equal(root.get(outer).get(inside), value).not();
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 为空 is null
         *
         * @param key 字段名
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> isNull(String key) {
            Specification<T> specification = (root, query, builder) -> builder.isNull(root.get(key));
            this.invoke(specification);
            return this;
        }

        /**
         * 为空 is null
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> isNull(String outer, String inside) {
            Specification<T> specification = (root, query, builder) -> builder.isNull(root.get(outer).get(inside));
            this.invoke(specification);
            return this;
        }

        /**
         * 不为空 not null
         *
         * @param key 字段名
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notNull(String key) {
            Specification<T> specification = (root, query, builder) -> builder.isNull(root.get(key)).not();
            this.invoke(specification);
            return this;
        }

        /**
         * 不为空 not null
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notNull(String outer, String inside) {
            Specification<T> specification = (root, query, builder) -> builder.isNull(root.get(outer).get(inside)).not();
            this.invoke(specification);
            return this;
        }

        /**
         * in
         *
         * @param key    字段名
         * @param values 值 (可传入一个List类型，或可变长参数)
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> in(String key, Object... values) {
            if (this.check(MatchType.In, values)) {
                Specification<T> specification;
                if (values[0] instanceof List) {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(key)).value(values[0]);
                } else {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(key)).value(Arrays.asList(values));
                }
                this.invoke(specification);
            }
            return this;
        }

        /**
         * in
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param values 值 (可传入一个List类型，或可变长参数)
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> in(String outer, String inside, Object... values) {
            if (this.check(MatchType.In, values)) {
                Specification<T> specification;
                if (values[0] instanceof Collection) {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(outer).get(inside)).value(values[0]);
                } else {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(outer).get(inside)).value(Arrays.asList(values));
                }
                this.invoke(specification);
            }
            return this;
        }

        /**
         * not in
         *
         * @param key    字段名
         * @param values 值 (可传入一个List类型，或可变长参数)
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notIn(String key, Object... values) {
            if (this.check(MatchType.In, values)) {
                Specification<T> specification;
                if (values[0] instanceof List) {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(key)).value(values[0]).not();
                } else {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(key)).value(Arrays.asList(values)).not();
                }
                this.invoke(specification);
            }
            return this;
        }

        /**
         * not in
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param values 值 (可传入一个List类型，或可变长参数)
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> notIn(String outer, String inside, Object... values) {
            if (this.check(MatchType.In, values)) {
                Specification<T> specification;
                if (values[0] instanceof List) {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(outer).get(inside)).value(values[0]).not();
                } else {
                    specification = (Specification<T>) (root, query, builder) -> builder.in(root.get(outer).get(inside)).value(Arrays.asList(values)).not();
                }
                this.invoke(specification);
            }
            return this;
        }

        /**
         * between (闭区间,值参数不分大小)
         *
         * @param key   字段名
         * @param upper 值 类型必须实现Comparable接口
         * @param lower 值 类型必须实现Comparable接口
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> between(String key, Comparable upper, Comparable lower) {
            if (this.check(MatchType.Between, new Object[]{upper, lower})) {
                Specification<T> specification = (root, query, builder) -> builder.between(root.get(key), upper, lower);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * between (闭区间,值参数不分大小)
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param upper  值 类型必须实现Comparable接口
         * @param lower  值 类型必须实现Comparable接口
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> between(String outer, String inside, Comparable upper, Comparable lower) {
            if (this.check(MatchType.Between, new Object[]{upper, lower})) {
                Specification<T> specification = (root, query, builder) -> builder.between(root.get(outer).get(inside), upper, lower);
                this.invoke(specification);
            }
            return this;
        }


        /**
         * 大于 (great than)
         *
         * @param key   字段名
         * @param value 值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> gt(String key, Number value) {
            if (this.check(MatchType.GT, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.gt((root.get(key)), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 大于 (great than)
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> gt(String outer, String inside, Number value) {
            if (this.check(MatchType.GT, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.gt(root.get(outer).get(inside), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 大于等于（great than or equal）
         *
         * @param key   字段名
         * @param value 值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> ge(String key, Number value) {
            if (this.check(MatchType.GE, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.ge(root.get(key), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 大于等于（great than or equal）
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> ge(String outer, String inside, Number value) {
            if (this.check(MatchType.GE, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.ge(root.get(outer).get(inside), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 小于 (less than)
         *
         * @param key   字段名
         * @param value 值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> lt(String key, Number value) {
            if (this.check(MatchType.LT, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.lt(root.get(key), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 小于(less than)
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> lt(String outer, String inside, Number value) {
            if (this.check(MatchType.LT, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.lt((root.get(outer).get(inside)), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 大于等于（less than or equal）
         *
         * @param key   字段名
         * @param value 值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> le(String key, Number value) {
            if (this.check(MatchType.LE, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.le((root.get(key)), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 大于等于（less than or equal）
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param value  值 类型必须继承Number
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> le(String outer, String inside, Number value) {
            if (this.check(MatchType.LT, new Object[]{value})) {
                Specification<T> specification = (Specification<T>) (root, query, builder) -> builder.le((root.get(outer).get(inside)), value);
                this.invoke(specification);
            }
            return this;
        }

        /**
         * 查询两个时间内的内容(日期格式为yyyy-MM-dd),DO中类型必须为java.Util.Date (如果为Instant,请使用betweenInstant)
         *
         * @param key  字段名
         * @param time 时间数组，包含两个时间（开始时间、结束时间）
         * @return SpecificationUtils 返回time[0]00:00:00-time[1]23:59:59闭区间的内容
         */
        public JpaSpecImpl<T> betweenDate(String key, List<String> time) {
            try {
                if (this.check(MatchType.BetweenTime, new Object[]{time})) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    // 转换时自动把时区时间转换成了世界时间
                    Date start = new Date(format.parse(time.get(0)).toInstant().toEpochMilli());
                    // 截止时间是结束那天的23时59分59秒
                    Date end = new Date(format.parse(time.get(1)).toInstant().plus(Duration.ofHours(23).plusMinutes(59).plusSeconds(59)).toEpochMilli());
                    Specification<T> specification = (root, query, builder) -> builder.between(root.get(key), start, end);
                    this.invoke(specification);
                }
                return this;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        /**
         * 查询两个时间内的内容(日期格式为yyyy-MM-dd),DO中类型必须为java.Util.Date (如果为Instant,请使用betweenInstant)
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param time   时间数组，包含两个时间（开始时间、结束时间）
         * @return SpecificationUtils 返回time[0]00:00:00-time[1]23:59:59闭区间的内容
         */
        public JpaSpecImpl<T> betweenDate(String outer, String inside, List<String> time) {
            try {
                if (this.check(MatchType.BetweenTime, new Object[]{time})) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    // 转换时自动把时区时间转换成了世界时间
                    Date start = new Date(format.parse(time.get(0)).toInstant().toEpochMilli());
                    // 截止时间是结束那天的23时59分59秒
                    Date end = new Date(format.parse(time.get(1)).toInstant().plus(Duration.ofHours(23).plusMinutes(59).plusSeconds(59)).toEpochMilli());
                    Specification<T> specification = (root, query, builder) -> builder.between(root.get(outer).get(inside), start, end);
                    this.invoke(specification);
                }
                return this;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        /**
         * 查询两个时间内的内容(日期格式为yyyy-MM-dd),DO中类型必须为Instant (如果Date,请使用betweenDate)
         *
         * @param key  字段名
         * @param time 时间数组，包含两个时间（开始时间、结束时间）
         * @return SpecificationUtils 返回time[0]00:00:00-time[1]23:59:59闭区间的内容
         */
        public JpaSpecImpl<T> betweenInstant(String key, List<String> time) {
            try {
                if (this.check(MatchType.BetweenTime, new Object[]{time})) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    // 转换时自动把时区时间转换成了世界时间
                    Instant start = format.parse(time.get(0)).toInstant();
                    // 截止时间是结束那天的23时59分59秒
                    Instant end = format.parse(time.get(1)).toInstant().plus(Duration.ofHours(23).plusMinutes(59).plusSeconds(59));
                    Specification<T> specification = (root, query, builder) -> builder.between(root.get(key), start, end);
                    this.invoke(specification);
                }
                return this;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        /**
         * 查询两个时间内的内容(日期格式为yyyy-MM-dd),DO中类型必须为Instant (Date,请使用betweenDate)
         *
         * @param outer  外层字段名
         * @param inside 内层字段名
         * @param time   时间数组，包含两个时间（开始时间、结束时间）
         * @return SpecificationUtils 返回time[0]00:00:00-time[1]23:59:59闭区间的内容
         */
        public JpaSpecImpl<T> betweenInstant(String outer, String inside, List<String> time) {
            try {
                if (this.check(MatchType.BetweenTime, new Object[]{time})) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    // 转换时自动把时区时间转换成了世界时间
                    Instant start = format.parse(time.get(0)).toInstant();
                    // 截止时间是结束那天的23时59分59秒
                    Instant end = format.parse(time.get(1)).toInstant().plus(Duration.ofHours(23).plusMinutes(59).plusSeconds(59));
                    Specification<T> specification = (root, query, builder) -> builder.between(root.get(outer).get(inside), start, end);
                    this.invoke(specification);
                }
                return this;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }


        /**
         * 切换里面的内容为or
         *
         * @param specification 查询条件
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> or(Specification<T> specification) {
            this.spec = this.spec.or(specification);
            return this;
        }

        /**
         * 切换里面的内容为and
         *
         * @param specification 查询条件
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> and(Specification<T> specification) {
            this.spec = this.spec.and(specification);
            return this;
        }

        /**
         * 有选择的执行and
         *
         * @param condition     是否执行specification
         * @param specification 查询条件
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> or(Boolean condition, Specification<T> specification) {
            if (condition) {
                this.spec = this.spec.or(specification);
            }
            return this;
        }

        /**
         * 有选择的执行or
         *
         * @param condition     是否执行specification
         * @param specification 查询条件
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> and(Boolean condition, Specification<T> specification) {
            if (condition) {
                this.spec = this.spec.and(specification);
            }
            return this;
        }

        /**
         * 切换后面内容为or
         *
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> or() {
            try {
                this.specMethod = this.spec.getClass().getMethod("or", Specification.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return this;
        }

        /**
         * 切换后面的内容为and
         *
         * @return SpecificationUtils
         */
        public JpaSpecImpl<T> and() {
            try {
                this.specMethod = this.spec.getClass().getMethod("and", Specification.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return this;
        }


        private String getLikePattern(String content) {
            return "%" + content.trim() + "%";
        }

        /**
         * 校验
         *
         * @param operator 操作符
         * @param values   值
         * @return 校验是否为空，操作符为ISNULL时不做校验
         */
        private Boolean check(MatchType operator, Object[] values) {
            switch (operator) {
                case LIKE:
                case EQUAL:
                    return ObjectUtils.isNotEmpty(values[0]);
                case GT:
                case GE:
                case LT:
                case LE:
                case BetweenTime:
                    return null != values[0];
                case Between:
                    if (values != null && values.length >= 2) {
                        return StringUtils.isNotBlank(values[0].toString()) && StringUtils.isNotBlank(values[1].toString());
                    } else {
                        return false;
                    }
                case In:
                    return ArrayUtils.isNotEmpty(values) && null != values[0];
                case ISNULL:
                default:
                    break;
            }
            return true;
        }

        public Specification<T> done() {
            Specification<T> tempSpec = this.spec;
            this.spec = Specification.where(null);
            return tempSpec;
        }


    }
}
