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

	requests := make([][]chan Msg, *m)
	for r := range requests {
		requests[r] = make([]chan Msg, n>>(r+1))
		for j := range requests[r] {
			requests[r][j] = make(chan Msg)
		}
	}
	champion := make(chan string)

	// processes: N-1 matches
	for r := 0; r < *m-1; r++ {
		for j, request := range requests[r] {
			go Match(r+1, j, request)
		}
	}
	go Final(*m, requests[*m-1][0], champion)

	// player i plays match i>>(r+1) in round r+1
	for i := 0; i < n; i++ {
		path := make([]chan<- Msg, *m)
		for r := range path {
			path[r] = requests[r][i>>(r+1)]
		}
		go Player(i, path)
	}

	fmt.Printf("Tournament winner: %s\n", <-champion)
}
