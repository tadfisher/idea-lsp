package com.example;

class Definition {
    private static final String CONST = "const";

    void referencePrivateConst() {
        String c = CONST;
        System.out.println(c);
    }

    void referencePackagePrivateMethod() {
        PackagePrivate pp = new PackagePrivate();
        pp.aMethod();
    }
}
