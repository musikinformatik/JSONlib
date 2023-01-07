# JSONlib

> A JSON de- and encoder for SuperCollider

The JSON implementation of the SuperCollider standard library lacks certain features such as

* [ ] lack of a JSON encoder (the conversion of an dictionary to a JSON string)
* [ ] parsing JSON values into their respective type (`.parseJSON` converts everything to a string)

which this Quark implements, building on top of the existing implementation.

There also exists a [json](https://github.com/supercollider-quarks/json) Quark which also adds a wrapper for [sccode.org](https://sccode.org) but lacks a recursive encoding of objects.
The goal of `JSONlib` is to simply provide a full implementation of the JSON standard in sclang and nothing beyond it.

## License

GPL-2.0
