# JSONlib

> A JSON de- and encoder for SuperCollider

The JSON implementation of the SuperCollider standard library lacks certain features such as

* [ ] lack of a JSON encoder (the conversion of an dictionary to a JSON string)
* [ ] parsing JSON values into their respective type (`.parseJSON` converts everything to a string)

which this Quark implements, building on top of the existing implementation.

There also exists a [json](https://github.com/supercollider-quarks/json) Quark which also adds a wrapper for [sccode.org](https://sccode.org) but lacks a recursive encoding.
This Quark just focusses on the full implementation of the JSON standard SuperCollider.

## License

GPL-2.0
