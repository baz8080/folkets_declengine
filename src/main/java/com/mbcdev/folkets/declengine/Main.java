package com.mbcdev.folkets.declengine;

import com.squareup.javapoet.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.lang.model.element.Modifier;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


/**
 * Test class
 *
 * Created by barry on 12/03/2017.
 */
public class Main {

    private static final String PACKAGE_NAME = "com.mbcdev.folkets.declengine";
    private static final String GENERATED_SOURCE_DIRECTORY = "src/gen/java";
    private static String[] DECLENSION_TYPES = new String[] {
            "sg indef nom", "sg indef gen",
            "sg def nom", "sg def gen",
            "pl indef nom", "pl indef gen",
            "pl def nom", "pl def gen"
    };


    public static void main(String[] args) throws Exception {
        generateNounDeclensionSources(args[0]);
    }

    private static void generateNounDeclensionSources(String nounsList) throws Exception {

        nounsList = nounsList == null ? "src/main/resources/noun_paradigms.txt" : nounsList;

        SaldoService saldoService = getService();

        BufferedReader bufferedReader =
                new BufferedReader(new FileReader(nounsList));
        String paradigm;

        TypeSpec.Builder factoryBuilder = TypeSpec.classBuilder("NounDeclensionFactory")
                .addJavadoc("Factory for {@link NounDeclension} objects")
                .addJavadoc("\n")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        MethodSpec.Builder factoryMethodBuilder = MethodSpec.methodBuilder("getInstance")
                .addJavadoc("Method to get an instance of {@link NounDeclension} for the given\n" +
                        "paradigm and word. The paradigm describes how the declensions are formed.\n" +
                        "For example, nn_0n_ansvar says that it is a noun, 0th declension form, and\n" +
                        "the word should follows the same rules as 'ansvar'\n")
                .addJavadoc("\n")
                .addJavadoc("@param paradigm the paradigm of the word\n")
                .addJavadoc("@param word the value of the word\n")
                .addJavadoc("@return an instance of NounDeclension for the given paradigm and word,\n" +
                        "        or null if one was not found")
                .addJavadoc("\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String.class, "paradigm")
                .addParameter(String.class, "word")
                .returns(ClassName.get(PACKAGE_NAME, "NounDeclension"));

        factoryMethodBuilder.beginControlFlow("if (paradigm == null || word == null)")
                .addStatement("return null")
                .endControlFlow();

        TypeSpec interfaceTypeSpec = generateNounDeclensionInterface();

        boolean first = true;

        while ((paradigm = bufferedReader.readLine()) != null) {

            log("Requesting paradigms for %s", paradigm);

            Call<List<SaldoNounDeclension>> call = saldoService.getNounDeclensions(paradigm);

            Response<List<SaldoNounDeclension>> response = call.execute();

            if (!response.isSuccessful()) {
                throw new IllegalStateException("Response was not successful.");
            }

            if (response.isSuccessful() && response.body() != null) {

                HashMap<String, String> typeFormMap = new HashMap<>();

                for (SaldoNounDeclension nounDeclension : response.body()) {

                    /*
                        Some forms have solutions that either take utrum or neutrum, so they ]
                        could end up like _et, _en
                     */
                    String existingForm = typeFormMap.get(nounDeclension.getType());

                    if (existingForm != null) {
                        existingForm = existingForm + ", " + nounDeclension.getForm();
                        typeFormMap.put(nounDeclension.getType(), existingForm);
                    } else {
                        typeFormMap.put(nounDeclension.getType(), nounDeclension.getForm());
                    }
                }

                if (first) {
                    factoryMethodBuilder.beginControlFlow("if (paradigm.equals($S))", paradigm);
                    first = false;
                } else {
                    factoryMethodBuilder.nextControlFlow("else if (paradigm.equals($S))", paradigm);
                }

                factoryMethodBuilder.addStatement("return new $LNounDeclension(word)", paradigm);


                TypeSpec.Builder concreteBuilder = TypeSpec.classBuilder(paradigm + "NounDeclension")
                        .addModifiers(Modifier.FINAL)
                        .addSuperinterface(ClassName.get(PACKAGE_NAME, interfaceTypeSpec.name))
                        .addField(String.class, "word", Modifier.PRIVATE, Modifier.FINAL);

                MethodSpec constructorSpec = MethodSpec.constructorBuilder()
                        .addParameter(String.class, "word")
                        .addStatement("this.word = word")
                        .build();

                concreteBuilder.addMethod(constructorSpec);

                for (String msd : DECLENSION_TYPES) {
                    concreteBuilder.addMethod(createNounMethod(msd, typeFormMap.get(msd)));
                }

                JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, concreteBuilder.build())
                        .build();

                javaFile.writeTo(new File(GENERATED_SOURCE_DIRECTORY));
            }

            Thread.sleep(50);
        }

        bufferedReader.close();

        factoryMethodBuilder.nextControlFlow("else")
                .addStatement("return null")
                .endControlFlow();

        factoryBuilder.addMethod(factoryMethodBuilder.build());
        JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, factoryBuilder.build())
                .build();
        javaFile.writeTo(new File(GENERATED_SOURCE_DIRECTORY));

    }

    private static TypeSpec generateNounDeclensionInterface() throws IOException {

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder("NounDeclension")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(generateNounDeclensionMethodSpec(
                        "getSingularIndefiniteNominative", "singular indefinite nominative"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getSingularIndefiniteGenitive", "singular indefinite genitive"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getSingularDefiniteNominative", "singular definitive nominative"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getSingularDefiniteGenitive", "singular definitive genitive"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getPluralIndefiniteNominative", "plural indefinite nominative"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getPluralIndefiniteGenitive", "plural indefinite genitive"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getPluralDefiniteNominative", "plural definitive nominative"))
                .addMethod(generateNounDeclensionMethodSpec(
                        "getPluralDefiniteGenitive", "plural definitive genitive"));

        TypeSpec interfaceTypeSpec = interfaceBuilder
                .addJavadoc("Defines behaviour of how declensions can be applied to a noun\n")
                .addJavadoc("<p>\n")
                .addJavadoc("These objects are obtained with the {@link NounDeclensionFactory}\n")
                .addJavadoc("</p>\n")
                .build();
        JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, interfaceTypeSpec)
                .build();

        javaFile.writeTo(new File(GENERATED_SOURCE_DIRECTORY));

        return interfaceTypeSpec;
    }

    private static MethodSpec generateNounDeclensionMethodSpec(String methodName, String javadocName) {

        CodeBlock javaDoc = CodeBlock.of(
                "Gets the $L form of a noun\n\n" +
                "@return the $L form of a noun\n", javadocName, javadocName);

        MethodSpec methodSpec = MethodSpec.methodBuilder(
                methodName)
                .addJavadoc(javaDoc)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(String.class)
                .build();

        return methodSpec;
    }

    private static MethodSpec createNounMethod(String msd, String form) {

        String methodName = getMethodName(msd);

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);

        if (form == null) {
            methodSpecBuilder.addStatement("return null");
        } else  {
            methodSpecBuilder.beginControlFlow("if (word == null)")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("return $S.replaceAll($S, word)", form, "_");
        }

        return methodSpecBuilder.build();
    }

    /**
     * Gets the method name to generate for the given declension type
     *
     * @param declensionType The declension type returned from the API
     * @return The method name to generate for the given declension type, or null if unknown
     */
    private static String getMethodName(String declensionType) {
        String name = null;

        switch (declensionType) {
            case "sg indef nom":
                name = "getSingularIndefiniteNominative";
                break;
            case "sg indef gen":
                name = "getSingularIndefiniteGenitive";
                break;
            case "sg def nom":
                name = "getSingularDefiniteNominative";
                break;
            case "sg def gen":
                name = "getSingularDefiniteGenitive";
                break;
            case "pl indef nom":
                name = "getPluralIndefiniteNominative";
                break;
            case "pl indef gen":
                name = "getPluralIndefiniteGenitive";
                break;
            case "pl def nom":
                name = "getPluralDefiniteNominative";
                break;
            case "pl def gen":
                name = "getPluralDefiniteGenitive";
                break;
        }

        return name;
    }

    private static SaldoService getService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://spraakbanken.gu.se")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(SaldoService.class);
    }

    private static void log(String format, Object... args) {
        if (args == null) {
            System.out.println(format);
        }

        System.out.println(String.format(Locale.US, format, args));
    }
}
