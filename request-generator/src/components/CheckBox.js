import React, {Component} from 'react';
import { connect } from 'react-redux';


class CheckBox extends Component {
    constructor(props){
        super(props);
        this.state={
            toggle: false
        };

    this.onInputChange = this.onInputChange.bind(this);

    }
    onInputChange(event){
        if(this.state.toggle){
            this.setState({toggle:false})
            this.props.updateCB(this.props.elementName, false);
        }else{
            this.setState({toggle:true})
            this.props.updateCB(this.props.elementName, true);

        }

    }
    render() {
        const toggleClass = this.state.toggle?"checkBoxClicked":"checkBox";
        const indicatorClass = this.state.toggle?"onOffActive":"onOff";
        return (
            <span>
            <button
            className={toggleClass +" btn-class btn"}
            name={this.props.elementName}
            onClick={this.onInputChange}
            >OAuth <a className={indicatorClass + " onOffState"} ></a></button>
            </span>
        )
    }
}

function mapStateToProps({value}){
    return {value};
}
export default connect(mapStateToProps)(CheckBox);