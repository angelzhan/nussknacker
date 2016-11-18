import React from 'react'
import { render, findDOMNode } from 'react-dom'
import joint from 'jointjs'
import EspNode from './EspNode'
import 'jointjs/dist/joint.css'
import _ from 'lodash'
import svgPanZoom from 'svg-pan-zoom'
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import ActionsUtils from '../../actions/ActionsUtils';
import NodeDetailsModal from './nodeDetailsModal.js';
import { DropTarget } from 'react-dnd';
import '../../stylesheets/graph.styl'


class Graph extends React.Component {

    static propTypes = {
        processToDisplay: React.PropTypes.object.isRequired,
        loggedUser: React.PropTypes.object.isRequired,
        connectDropTarget: React.PropTypes.func.isRequired
    }

    constructor(props) {
        super(props);
        this.graph = new joint.dia.Graph();
        this.graph
          .on("remove", (e, f) => {
            if (e.isLink) {
              this.props.actions.nodesDisconnected(e.attributes.source.id, e.attributes.target.id)
            }
        })
    }

    addNode(node, position) {
      this.props.actions.nodeAdded(node, position);
    }

    componentDidMount() {
        this.processGraphPaper = this.createPaper()
        this.drawGraph(this.props.processToDisplay)
        this.panAndZoom = this.enablePanZoom();
        this.changeNodeDetailsOnClick(this.processGraphPaper);
        this.labelToFrontOnHover(this.processGraphPaper);
        this.cursorBehaviour(this.processGraphPaper);
    }

    componentWillUpdate(nextProps, nextState) {
      const nothingChanged = _.isEqual(this.props.processToDisplay, nextProps.processToDisplay) &&
        _.isEqual(this.props.layout, nextProps.layout) &&
        _.isEqual(this.props.nodeToDisplay, nextProps.nodeToDisplay)
      if (!nothingChanged) {
        this.drawGraph(nextProps.processToDisplay, nextProps.layout, nextProps.nodeToDisplay)
      }
    }

    directedLayout() {
        joint.layout.DirectedGraph.layout(this.graph, {
            nodeSep: 200,
            edgeSep: 500,
            rankSep: 100,
            minLen: 300,
            rankDir: "TB"
        });
        this.changeLayoutIfNeeded()
    }

    nodeInputs = (nodeId) => {
      return this.props.processToDisplay.edges.filter(e => e.to == nodeId)
    }

    nodeOutputs = (nodeId) => {
      return this.props.processToDisplay.edges.filter(e => e.from == nodeId)
    }

    isMultiOutput = (nodeId) => {
      var node = this.props.processToDisplay.nodes.find(n => n.id == nodeId)
      return node.type == "Split"
    }

    //we don't allow multi outputs other than split, and no multiple inputs
    validateConnection = (cellViewS, magnetS, cellViewT, magnetT) => {
      var from = cellViewS.model.id
      var to = cellViewT.model.id

      var targetHasNoInput = this.nodeInputs(to).length == 0
      var sourceHasNoOutput = this.nodeOutputs(from).length == 0
      var canLinkFromSource = sourceHasNoOutput || this.isMultiOutput(from)

      return magnetT && targetHasNoInput && canLinkFromSource;
    }

    createPaper = () => {
        const canWrite = this.props.loggedUser.canWrite
        return new joint.dia.Paper({
            el: this.refs.espGraph,
            gridSize: 1,
            height: this.refs.espGraph.offsetHeight,
            width: this.refs.espGraph.offsetWidth,
            model: this.graph,
            snapLinks: { radius: 75 },
            interactive: function(cellView) {
                if (!canWrite) {
                  return false;
                } else if (cellView.model instanceof joint.dia.Link) {
                    // Disable the default vertex add functionality on pointerdown.
                    return { vertexAdd: false };
                } else {
                  return true;
                }
            },
            linkPinning: false,
            defaultLink: EspNode.makeLink({}),
            validateConnection: this.validateConnection
        })
          .on("cell:pointerup", (c, e) => {
            this.changeLayoutIfNeeded()
          })
          .on("link:connect", (c) => this.props.actions.nodesConnected(c.sourceView.model.id, c.targetView.model.id))
    }

    drawGraph = (data, layout, nodeToDisplay) => {
      const nodes = _.map(data.nodes, (n) => { return EspNode.makeElement(n) });
      const edges = _.map(data.edges, (e) => { return EspNode.makeLink(e) });
      const cells = nodes.concat(edges);
      this.graph.resetCells(cells);
      this.highlightNodes(data, nodeToDisplay);
      if (!layout) {
        this.directedLayout()
      } else {
        _.forEach(layout, el => this.graph.getCell(el.id).set('position', el.position));
      }
    }

    highlightNodes = (data, nodeToDisplay) => {
      _.keys(data.validationResult.invalidNodes).forEach(name => { this.highlightNode(name, 'node-validation-error') });
      if (nodeToDisplay) {
        this.highlightNode(nodeToDisplay.id, 'node-focused')
      }
    }

    highlightNode = (nodeId, highlightClass) => {
      const cell = this.graph.getCell(nodeId)
      if (cell) { //zabezpieczenie przed dostaniem sie do node'a propertiesow procesu, ktorego to nie pokazujemy na grafie
        this.processGraphPaper.findViewByModel(cell).highlight(null, { highlighter: { name: 'addClass', options: { className: highlightClass}} })
      }
    }

  changeLayoutIfNeeded = () => {
      var newLayout = _.map(this.graph.getElements(), (el) => {
              var pos = el.get('position');
              return { id: el.id, position: pos }
            })
      if (!_.isEqual(this.props.layout, newLayout)) {
        this.props.actions.layoutChanged(newLayout)
      }
    }

    enablePanZoom() {
        var panAndZoom = svgPanZoom(this.refs.espGraph.childNodes[0],
            {
                viewportSelector: this.refs.espGraph.childNodes[0].childNodes[0],
                fit: true,
                zoomScaleSensitivity: 0.4,
                controlIconsEnabled: true,
                panEnabled: false,
                dblClickZoomEnabled: false
            });
        this.processGraphPaper.on('blank:pointerdown', (evt, x, y) => {
            panAndZoom.enablePan();
        });
        this.processGraphPaper.on('cell:pointerup blank:pointerup', (cellView, event) => {
            panAndZoom.disablePan();
        });
        this.centerGraphHack(panAndZoom)
        return panAndZoom
    }

    //fixme To jest niestety hack. Po dodaniu prawego panelu graf nie jest juz wysrodkowany, ten hack to w ulomny sposob naprawia.
    //Docelowe rozwiazanie to przerobienie layoutu, tak zeby w widoku grafu elementy nie nachodzily na siebie (tak jak prawy panel nachodzi na graf)
    //Fajnie jakby prawy panel dalej byl resizowalny, to troche utrudnia sprawe, stad takie szybkie rozwiazanie
    centerGraphHack(panAndZoom) {
      const currentPan = panAndZoom.getPan()
      panAndZoom.pan({x: currentPan.x - 270/2, y: currentPan.y})
    }

    changeNodeDetailsOnClick () {
      this.processGraphPaper.on('cell:pointerdblclick', (cellView, evt, x, y) => {
        if (cellView.model.attributes.nodeData) {
          this.props.actions.displayModalNodeDetails(cellView.model.attributes.nodeData)
        }
      })
      this.processGraphPaper.on('cell:pointerclick', (cellView, evt, x, y) => {
        if (cellView.model.attributes.nodeData) {
          this.props.actions.displayNodeDetails(cellView.model.attributes.nodeData)
        }
      });
    }

    labelToFrontOnHover () {
        this.processGraphPaper.on('cell:mouseover', (cellView, evt, x, y) => {
          cellView.model.toFront();
        });
    }
    // FIXME - w chrome 52.0.2743.82 (64-bit) nie działa na esp-graph
    // Trzeba sprawdzić na innych wersjach Chrome u innych, bo może to być kwestia tylko tej wersji chrome
    cursorBehaviour () {
      this.processGraphPaper.on('blank:pointerdown', (evt, x, y) => {
        if (this.refs.espGraph) {
          this.refs.espGraph.style.cursor = "move"
        }
      })
      this.processGraphPaper.on('blank:pointerup', (evt, x, y) => {
        if (this.refs.espGraph) {
          this.refs.espGraph.style.cursor = "auto"
        }
      })
    }

    render() {
        return this.props.connectDropTarget(
            <div>
                <h2 id="process-name">{this.props.processToDisplay.id}</h2>
                {!_.isEmpty(this.props.nodeToDisplay) ? <NodeDetailsModal/> : null }
                <div ref="espGraph" id="esp-graph"></div>
                <button type="button" className="btn btn-default hidden" onClick={this.directedLayout}>Directed layout</button>
            </div>
        );

    }
}

function mapState(state) {
    return {
        nodeToDisplay: state.graphReducer.nodeToDisplay,
        processToDisplay: state.graphReducer.processToDisplay,
        loggedUser: state.settings.loggedUser,
        layout: state.graphReducer.layout
    };
}

var spec = {
  drop: (props, monitor, component) => {
    const pan = component.panAndZoom.getPan()
    const sizes = component.panAndZoom.getSizes()
    var pointerOffset = monitor.getClientOffset()
    var rect = findDOMNode(component).getBoundingClientRect();
    var zoom = component.panAndZoom.getSizes().realZoom
    //czegos tu chyba jeszcze brakuje... ale nie wiem czego :|
    var relOffset = { x: (pointerOffset.x - rect.left - pan.x)/zoom, y : (pointerOffset.y - rect.top - pan.y)/zoom }
    console.log(relOffset)

    component.addNode(monitor.getItem(), relOffset)

  }
};

//withRef jest po to, zeby parent mogl sie dostac
export default connect(mapState, ActionsUtils.mapDispatchWithEspActions, null, {withRef: true})(DropTarget("element", spec, (connect, monitor) => ({
  connectDropTarget: connect.dropTarget()
}))(Graph));

