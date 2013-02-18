package dct

class User {
    String name

    static constraints = {
        name(blank:false)
    }
}
