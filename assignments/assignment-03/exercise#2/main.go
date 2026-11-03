package main

import (
	"flag"
	"fmt"
	"os"
)

func main() {
	m := flag.Int("m", 4, "number of rounds: the game has N = 2^m players")
	flag.Parse()
	if *m < 1 || *m > 10 {
		fmt.Fprintln(os.Stderr, "m must be between 1 and 10")
		os.Exit(2)
	}
	n := 1 << *m

	done := make(chan struct{})
	defer close(done) // stops every goroutine still alive (e.g. the relays)

	players := make([]<-chan Msg, n)
	for i := range players {
		players[i] = addPlayer(fmt.Sprintf("player-%d", i), done)
	}

	fmt.Printf("Tournament winner: %s\n", <-Tournament(players, done))
}
