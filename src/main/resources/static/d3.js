// import * as d3 from 'https://d3js.org/d3.v3.min.js';

function chart(data) {
// https://observablehq.com/@garciaguillermoa/force-directed-graph
var width = 800, height = 600;

drag = simulation => {

    function dragstarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }

    function dragended(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }

    return d3.drag()
        .on("start", dragstarted)
        .on("drag", dragged)
        .on("end", dragended);
}

var svg = d3.select("#graph").append("svg")
    .attr("width", width).attr("height", height);

// d3.json("/graph").then( function (data) {
    console.log(data);

    const simulation = d3.forceSimulation(data.nodes)
        .force("link", d3.forceLink(data.links)
            .id(d => d.id)
        )
        .force("charge", d3.forceManyBody()
            .strength(-7)
        )
        .force("center", d3.forceCenter(width / 2, height / 2))
        .force("boundary", forceBoundary(0, 0, width, height)
            .border(10)
        );

    const link = svg.append("g")
        .attr("stroke", "#999")
        .attr("stroke-opacity", 0.6)
        .selectAll("line")
        .data(data.links)
        .join("line");

    const node = svg.append("g")
        .selectAll(".node")
        .data(data.nodes)
        .join("g")
        .attr('class', 'node')
        .call(drag(simulation));

    node.append('circle')
        .attr("r", 5);

    node.append("text")
        .text(function (d) {
            return d.title;
        })
        .style('fill', '#000')
        .style('font-size', '12px')
        .attr('x', 6)
        .attr('y', 3);

    const x0 = simulation.force("boundary").x0()(),
        x1 = simulation.force("boundary").x1()(),
        y0 = simulation.force("boundary").y0()(),
        y1 = simulation.force("boundary").y1()(),
        b = simulation.force("boundary").border()();

    svg.append("rect")
        .attr("x", x0)
        .attr("y", y0)
        .attr("width", x1 - x0)
        .attr("height", y1 - y0)
        .style("fill", "none")
        .style("stroke", "red");

    simulation.on("tick", () => {
        link
            .attr("x1", d => d.source.x)
            .attr("y1", d => d.source.y)
            .attr("x2", d => d.target.x)
            .attr("y2", d => d.target.y);

        node
            .attr("transform", d => `translate(${d.x}, ${d.y})`);
    });
}
// });