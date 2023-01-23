# JSONlib

> A JSON de- and encoder for SuperCollider

The JSON implementation of the SuperCollider standard library lacks certain features such as

* [ ] a JSON encoder (the conversion of an dictionary to a JSON string)
* [ ] parsing JSON values into their respective type (`.parseJSON` converts everything to a string)

which this Quark implements, building on top of the existing implementation.

There also exists a [json](https://github.com/supercollider-quarks/json) Quark which also adds a wrapper for [sccode.org](https://sccode.org) but lacks a recursive encoding of objects.
The goal of `JSONlib` is to simply provide a full implementation of the JSON standard in sclang and nothing beyond it.

## Quickstart

### Installation

```supercollider
// install the quark
Quarks.install("https://github.com/musikinformatik/JSONlib.git");

// restart the interpreter so the new classes are available
thisProcess.recompile;

// open documention
HelpBrowser.openHelpFor("Classes/JSONlib");
```

### Basic usage

#### Parse a JSON

Let's say we have a JSON with an integer as a value

```json
{
    "hello": 42
}
```

which we want to parse in sclang.

```supercollider
// use Symbol instead of String to get rid of escaping quotation marks
j = '{"hello": 42}';

// turn this into an Event
d = JSONlib.convertToSC(j);
// -> ( 'hello': 42 )

// an integer gets parsed as an integer
d[\hello].class
// -> Integer

// compare to the built-in method of sclang
// it uses a Dictionary instead of an Event
d = j.parseJSON()
// -> Dictionary[ (hello -> 42) ]

// but 42 is a string here
d["hello"].class
// -> String
```

#### Encode an Event as JSON

```supercollider
// create an event
e = (\foo: "bar", \baz: (\nested: true));

e.asJSON
// -> { "baz": { "nested": true }, "foo": "bar" }

// or use the class JSONlib
JSONlib.convertToJSON(e);
// -> { "baz": { "nested": true }, "foo": "bar" }
```

Advanced usage is described in the SCdoc documentation.

## Development

Make sure to run the tests via

```supercollider
TestJSONlib.run;
```

## License

GPL-2.0
