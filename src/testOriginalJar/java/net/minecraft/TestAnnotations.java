package net.minecraft;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jws.WebService;


@Nullable
@WebService(endpointInterface = "somevalue")
public class TestAnnotations extends TestAbstractClass {
    public TestAnnotations() {
        super(0, null);
    }

    @Override
    @Nullable
    public TestAbstractClass abstractMethod() {
        return null;
    }

    @Override
    @NotNull
    public TestAbstractClass abstractMethodParam(TestConcreteClass x) {
        return null;
    }

    public void foo(@Nullable int param1, @NotNull String param2) {

    }

    @Nullable
    public String nullable() {
        return "Asdf";
    }

    @Nullable
    public String instanceField;

    @Nullable
    public static String staticField;

    @Nullable
    public static final String finalStaticField = "bar";

    @Override
    public TestInterface foo() {
        return null;
    }

    @Override
    public TestOtherClass boz(TestOtherClass x) {
        return null;
    }
}
