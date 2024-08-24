package com.ninestar.datapie.datamagic.service;

public class TfTranslator {
    /*
    private static final List<String> CLASSES = Arrays.asList("Normal", "Pneumonia");

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        NDManager manager = ctx.getNDManager();
        NDArray array = input.toNDArray(manager, Image.Flag.COLOR);
        array = NDImageUtils.resize(array, 224).div(255.0f);
        return new NDList(array);
    }

    @Override
    public Classifications processOutput(TranslatorContext ctx, NDList list) {
        NDArray probabilities = list.singletonOrThrow();
        return new Classifications(CLASSES, probabilities);
    }

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }

    */

}
