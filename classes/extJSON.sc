+ Collection {
	asJson {
		^this.asDict.asJson
	}
}

+ Dictionary {
	asJson {
		^JSONlib.convertToJSON(this)
	}
}
