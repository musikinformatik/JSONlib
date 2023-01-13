+ Collection {
	asJson {
		^this.asDict.asJson
	}
}

+ Dictionary {
	asJson {
		^JSONlib.dumps(this)
	}
}
