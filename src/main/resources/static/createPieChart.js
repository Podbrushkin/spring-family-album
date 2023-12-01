function createPieChart(data) {
    console.log('pie: data1:' + data);
    data = convertTsvToArrayOfObjects(data);
    console.log('pie: data2:');
    console.log(data);
    // data = findNumColumnAndRename(data);
    data = transformArray(data);
    console.log('pie: data3:');
    console.log(data);
    data = combineObjectsIntoOther(data, 20);
    console.log('pie: data3:' + data);
    // console.log('PIE CHART graph.size=' + data);
    // console.log(data);
    var dom = document.getElementById('containerPie');
    var myChart = echarts.init(dom, null, {
        renderer: 'canvas',
        useDirtyRect: false
    });
    var app = {};

    var option;

    option = {
        title: {
            // text: 'Referer of a Website',
            // subtext: 'Fake Data',
            // left: 'center'
        },
        tooltip: {
            trigger: 'item'
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            type: 'scroll',
            show: false
        },
        series: [
            {
                // name: 'Access From',
                type: 'pie',
                radius: '50%',
                data: data,
                label: {
                    // show: false
                },
                labelLayout: {
                    hideOverlap: true
                },
                // [
                //     { value: 1048, name: 'Search Engine' },
                //     { value: 735, name: 'Direct' },
                //     { value: 580, name: 'Email' },
                //     { value: 484, name: 'Union Ads' },
                //     { value: 300, name: 'Video Ads' }
                // ],
                // emphasis: {
                //     itemStyle: {
                //         shadowBlur: 10,
                //         shadowOffsetX: 0,
                //         shadowColor: 'rgba(0, 0, 0, 0.5)'
                //     }
                // }
            }
        ]
    };

    if (option && typeof option === 'object') {
        myChart.setOption(option);
    }

    window.addEventListener('resize', myChart.resize);
    function convertTsvToArrayOfObjects(tsv) {
        const lines = tsv.split('\n');
        const headers = lines[0].split('\t');
        const result = [];

        for (let i = 1; i < lines.length; i++) {
            const values = lines[i].split('\t');
            const obj = {};

            for (let j = 0; j < headers.length; j++) {
                obj[headers[j]] = values[j];
            }

            result.push(obj);
        }

        return result;
    }
    function combineObjectsIntoOther(items, threshold) {
        const sortedItems = items.sort((a, b) => b.value - a.value);

        const totalValue = items.reduce((acc, item) => acc + Number(item.value), 0);
        const significanceFactor = 0.01;
        threshold = totalValue * significanceFactor;
        console.log('threshold=' + threshold);

        // Create 'Other' category and filter out significant items
        const other = { name: 'Other', value: 0 };
        const significantItems = [];

        sortedItems.forEach(item => {
            if (Number(item.value) < threshold) {
                other.value += Number(item.value);
            } else {
                significantItems.push(item);
            }
        });

        // Include 'Other' category if it has a value greater than 0
        if (other.value > 0) {
            significantItems.push(other);
        }
        return significantItems;
    }
    // it finds numerical column and changes prop names so they will be recognised by echarts:
    function transformArray(arr) {
        if (arr.length === 0) {
            return [];
        }

        // Identify the property with a numerical value and one to rename to "name"
        let numProp, nameProp;
        for (const prop in arr[0]) {
            const val = arr[0][prop];

            // Check if the property is numerical
            if (!numProp && !isNaN(parseFloat(val)) && isFinite(val)) {
                numProp = prop;
            }

            if (!numProp || numProp !== prop) {
                nameProp = prop;
            }
        }

        if (!numProp) {
            // No numerical property found, return original array
            return arr;
        }

        // Rename the numerical property to "value" and another property to "name"
        return arr.map(obj => {
            const newObj = { ...obj };

            if (newObj[numProp] !== undefined) {
                newObj.value = parseFloat(newObj[numProp]);
                delete newObj[numProp];
            }

            if (nameProp && numProp !== nameProp) {
                // Only rename to "name" if there isn't already a "name" property
                if (!newObj.hasOwnProperty('name')) {
                    newObj.name = newObj[nameProp];
                    delete newObj[nameProp];
                }
            }

            return newObj;
        });
    }
}