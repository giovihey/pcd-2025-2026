package main

import (
	"math/rand/v2"
)

type Msg struct {
	value    int
	playerId string
}

func Player(ch chan Msg, id string) {
	pick := rand.IntN(5) + 1
	msg := Msg{value: pick, playerId: id}
	ch <- msg
}

func addPlayer(id string) chan Msg {
	ch := make(chan Msg)
	go Player(ch, id)
	return ch
}
