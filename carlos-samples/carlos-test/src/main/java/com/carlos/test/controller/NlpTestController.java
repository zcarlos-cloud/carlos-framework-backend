package com.carlos.test.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.Result;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dependency.nnparser.NeuralNetworkDependencyParser;
import com.hankcs.hanlp.model.perceptron.CWSTrainer;
import com.hankcs.hanlp.model.perceptron.PerceptronTrainer;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("nlp")
@Tag(name = "自然语言处理")
@Slf4j
public class NlpTestController {


    @GetMapping("keyword")
    @Operation(summary = "关键词提")
    public Result<?> keyword(String text) {
        if (StrUtil.isBlank(text)) {
            text = "来话人投诉新津区长绍号中国石油八达加油站对面的小路进00米左右，四川新津工业园区有一户商家（没有招牌和名字），从事车辆的发动机和电瓶的拆卸，但是存在极大噪音污染，拆卸的部件流出的机油流入旁边小溪，造成河水污染。并且该拆卸点有浓郁刺鼻的味道。市民已向镇政府和镇环保局反馈过问题，但是均告知不在其受理范围，希望相关部门核实情况，尽快督办整改";
        }
        List<String> keywordList = HanLP.extractKeyword(text, 50);
        return Result.success(keywordList);
    }

    @GetMapping("segment")
    @Operation(summary = "分词")
    public Result<?> segment(String text) {
        if (StrUtil.isBlank(text)) {
            text = "来话人投诉新津区长绍号中国石油八达加油站对面的小路进00米左右，四川新津工业园区有一户商家（没有招牌和名字），从事车辆的发动机和电瓶的拆卸，但是存在极大噪音污染，拆卸的部件流出的机油流入旁边小溪，造成河水污染。并且该拆卸点有浓郁刺鼻的味道。市民已向镇政府和镇环保局反馈过问题，但是均告知不在其受理范围，希望相关部门核实情况，尽快督办整改";
        }

        log.info("{}", JSONUtil.toJsonPrettyStr(HanLP.segment(text)));
        return Result.success();
    }


    @GetMapping("extractPhrase")
    @Operation(summary = "短语提取")
    public Result<?> extractPhrase(String text) {
        if (StrUtil.isBlank(text)) {
            text = "来话人投诉新津区长绍号中国石油八达加油站对面的小路进00米左右，四川新津工业园区有一户商家（没有招牌和名字），从事车辆的发动机和电瓶的拆卸，但是存在极大噪音污染，拆卸的部件流出的机油流入旁边小溪，造成河水污染。并且该拆卸点有浓郁刺鼻的味道。市民已向镇政府和镇环保局反馈过问题，但是均告知不在其受理范围，希望相关部门核实情况，尽快督办整改";
        }
        return Result.success(HanLP.extractPhrase(text, 20));
    }


    @GetMapping("compute")
    @Operation(summary = "依存句法分析")
    public Result<?> compute(String text) {
        if (StrUtil.isBlank(text)) {
            text = "来话人投诉新津区长绍号中国石油八达加油站对面的小路进00米左右，四川新津工业园区有一户商家（没有招牌和名字），从事车辆的发动机和电瓶的拆卸，但是存在极大噪音污染，拆卸的部件流出的机油流入旁边小溪，造成河水污染。并且该拆卸点有浓郁刺鼻的味道。市民已向镇政府和镇环保局反馈过问题，但是均告知不在其受理范围，希望相关部门核实情况，尽快督办整改";
        }
        return Result.success(NeuralNetworkDependencyParser.compute(text)
        );
    }


    @GetMapping("extractSummary")
    @Operation(summary = "自动摘要")
    public Result<?> extractSummary(String text) {
        if (StrUtil.isBlank(text)) {
            text = "来话人投诉新津区长绍号中国石油八达加油站对面的小路进00米左右，四川新津工业园区有一户商家（没有招牌和名字），从事车辆的发动机和电瓶的拆卸，但是存在极大噪音污染，拆卸的部件流出的机油流入旁边小溪，造成河水污染。并且该拆卸点有浓郁刺鼻的味道。市民已向镇政府和镇环保局反馈过问题，但是均告知不在其受理范围，希望相关部门核实情况，尽快督办整改";
        }
        return Result.success(HanLP.extractSummary(text, 3)
        );
    }


    @GetMapping("addressSeg")
    @Operation(summary = "地名提取")
    public Result<?> addressSeg(String text) {
        if (StrUtil.isBlank(text)) {
            text = "来话人投诉新津区长绍号中国石油八达加油站对面的小路进00米左右，四川新津工业园区有一户商家（没有招牌和名字），从事车辆的发动机和电瓶的拆卸，但是存在极大噪音污染，拆卸的部件流出的机油流入旁边小溪，造成河水污染。并且该拆卸点有浓郁刺鼻的味道。市民已向镇政府和镇环保局反馈过问题，但是均告知不在其受理范围，希望相关部门核实情况，尽快督办整改";
        }
        Segment segment = HanLP.newSegment().enablePlaceRecognize(true);
        List<Term> termList = segment.seg(text);
        log.info("地名提取结果：{}", JSONUtil.toJsonPrettyStr(termList));
        return Result.success();
    }

    @GetMapping("train/segment")
    @Operation(summary = "分词训练")
    public void segmentTrain() throws IOException {
        PerceptronTrainer trainer = new CWSTrainer();
        PerceptronTrainer.Result result = trainer.train(
            "data/test/pku98/199801.txt",
            ""
        );
        // System.out.printf("准确率F1:%.2f\n", result.prf[2]);
    }


}
