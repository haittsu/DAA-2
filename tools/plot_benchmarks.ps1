param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms.DataVisualization

$root = Split-Path -Parent $PSScriptRoot
$csvPath = Join-Path $root 'results\tables\benchmark.csv'
$plotDir = Join-Path $root 'results\plots'
$rows = @(Import-Csv -LiteralPath $csvPath)
$sizes = @(100, 1000, 10000, 100000)
$invariant = [System.Globalization.CultureInfo]::InvariantCulture

function Read-Number($row, [string]$column) {
    $value = $row.$column
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Missing $column for $($row.workload)/$($row.operation)/$($row.structure), n=$($row.n)"
    }
    return [double]::Parse($value, $invariant)
}

function Get-BenchmarkRows([string]$workload, [string]$operation,
        [string]$structure) {
    $selected = @($rows | Where-Object {
        $_.workload -eq $workload -and $_.operation -eq $operation -and
        $_.structure -eq $structure
    } | Sort-Object { [int]$_.n })
    if ($selected.Count -ne 4) {
        throw "Expected four rows for $workload/$operation/$structure"
    }
    for ($i = 0; $i -lt 4; $i++) {
        if ([int]$selected[$i].n -ne $sizes[$i]) {
            throw "Missing n=$($sizes[$i]) for $workload/$operation/$structure"
        }
    }
    return $selected
}

$definitions = @(
    @('random_access', 'get', 'DynamicArray'),
    @('random_access', 'get', 'LinkedList'),
    @('search', 'contains', 'DynamicArray'),
    @('search', 'contains', 'LinkedList'),
    @('priority_processing', 'insert', 'MinHeap'),
    @('priority_processing', 'extract_min', 'MinHeap')
)
foreach ($operation in @('insert_front', 'remove_front', 'insert_middle', 'remove_middle')) {
    $definitions += ,@('insertion_removal', $operation, 'DynamicArray')
    $definitions += ,@('insertion_removal', $operation, 'LinkedList')
}
if ($rows.Count -ne $definitions.Count * $sizes.Count) {
    throw "Incomplete CSV: expected $($definitions.Count * $sizes.Count) rows, found $($rows.Count)"
}

foreach ($definition in $definitions) {
    $selected = @(Get-BenchmarkRows $definition[0] $definition[1] $definition[2])
    foreach ($row in $selected) {
        if ($row.seed -ne '42' -or $row.repetitions -ne '5') {
            throw "Wrong seed or repetition count at n=$($row.n)"
        }
        $expectedOperations = switch ($row.workload) {
            'random_access' { 10000 }
            'search' { 1000 }
            'insertion_removal' { 1000 }
            'priority_processing' { [int]$row.n }
        }
        if ([int]$row.operations -ne $expectedOperations) {
            throw "Wrong operation count for $($row.workload), n=$($row.n)"
        }
        $metrics = @('avg_time_ns')
        if ($row.structure -eq 'DynamicArray') {
            $metrics += 'avg_element_accesses'
            if ($row.workload -eq 'search') { $metrics += 'avg_comparisons' }
            if ($row.workload -eq 'insertion_removal') { $metrics += 'avg_movements' }
        } elseif ($row.structure -eq 'LinkedList') {
            $metrics += 'avg_node_visits'
            if ($row.workload -eq 'search') { $metrics += 'avg_comparisons' }
        } else {
            $metrics += 'avg_comparisons'
        }
        foreach ($metric in $metrics) {
            $number = Read-Number $row $metric
            if ($number -lt 0 -or ($metric -eq 'avg_time_ns' -and $number -le 0)) {
                throw "Invalid $metric for $($row.workload), n=$($row.n)"
            }
            $fiveRunTotal = $number * 5
            if ([math]::Abs($fiveRunTotal - [math]::Round($fiveRunTotal)) -gt 0.0001) {
                throw "$metric is not an average of five integer measurements"
            }
        }
    }
}

function Series-Definition([string]$label, [string]$workload,
        [string]$operation, [string]$structure, [string]$metric,
        [string]$color, [string]$dash = 'Solid',
        [string]$chartType = 'Line', [string]$marker = 'Circle') {
    return [pscustomobject]@{
        Label = $label
        Workload = $workload
        Operation = $operation
        Structure = $structure
        Metric = $metric
        Color = $color
        Dash = $dash
        ChartType = $chartType
        Marker = $marker
    }
}

function Save-Plot([string]$fileName, [string]$title, [string]$axisTitle,
        [array]$definitions, [bool]$timePlot, [bool]$logY,
        [string]$note = '') {
    $chart = [System.Windows.Forms.DataVisualization.Charting.Chart]::new()
    try {
        $chart.Width = 1200
        $chart.Height = 720
        $chart.BackColor = [System.Drawing.Color]::White
        $chart.AntiAliasing = [System.Windows.Forms.DataVisualization.Charting.AntiAliasingStyles]::All

        $area = [System.Windows.Forms.DataVisualization.Charting.ChartArea]::new('main')
        $area.BackColor = [System.Drawing.Color]::White
        $area.AxisX.Title = 'Input size n (log scale)'
        $area.AxisX.TitleFont = [System.Drawing.Font]::new('Segoe UI', 11)
        $area.AxisX.LabelStyle.Font = [System.Drawing.Font]::new('Segoe UI', 10)
        $area.AxisX.LabelStyle.Format = '#,##0'
        $area.AxisX.IsLogarithmic = $true
        $area.AxisX.LogarithmBase = 10
        $area.AxisX.Minimum = 100
        $area.AxisX.Maximum = 100000
        $area.AxisX.MajorGrid.LineColor = [System.Drawing.Color]::FromArgb(225, 230, 236)
        $area.AxisX.MajorGrid.LineDashStyle = 'Dash'
        $area.AxisY.Title = $axisTitle
        $area.AxisY.TitleFont = [System.Drawing.Font]::new('Segoe UI', 11)
        $area.AxisY.LabelStyle.Font = [System.Drawing.Font]::new('Segoe UI', 10)
        $area.AxisY.IsLogarithmic = $logY
        $area.AxisY.LogarithmBase = 10
        $area.AxisY.MajorGrid.LineColor = [System.Drawing.Color]::FromArgb(225, 230, 236)
        $area.AxisY.MajorGrid.LineDashStyle = 'Dash'
        if ($timePlot) {
            $area.AxisY.LabelStyle.Format = '0.###'
        } else {
            $area.AxisY.LabelStyle.Format = '#,##0'
        }
        if (-not $logY) { $area.AxisY.Minimum = 0 }
        $chart.ChartAreas.Add($area) | Out-Null

        $mainTitle = [System.Windows.Forms.DataVisualization.Charting.Title]::new()
        $mainTitle.Text = $title
        $mainTitle.Font = [System.Drawing.Font]::new('Segoe UI', 16,
                [System.Drawing.FontStyle]::Bold)
        $mainTitle.ForeColor = [System.Drawing.Color]::FromArgb(26, 40, 60)
        $chart.Titles.Add($mainTitle) | Out-Null
        if ($note -ne '') {
            $subTitle = [System.Windows.Forms.DataVisualization.Charting.Title]::new()
            $subTitle.Text = $note
            $subTitle.Font = [System.Drawing.Font]::new('Segoe UI', 10)
            $subTitle.ForeColor = [System.Drawing.Color]::FromArgb(80, 90, 105)
            $chart.Titles.Add($subTitle) | Out-Null
        }

        $legend = [System.Windows.Forms.DataVisualization.Charting.Legend]::new('series')
        $legend.Docking = 'Bottom'
        $legend.Alignment = 'Center'
        $legend.Font = [System.Drawing.Font]::new('Segoe UI', 10)
        $legend.IsTextAutoFit = $false
        $chart.Legends.Add($legend) | Out-Null

        $minimumPositive = [double]::PositiveInfinity
        $maximumValue = 0.0
        foreach ($definition in $definitions) {
            $series = [System.Windows.Forms.DataVisualization.Charting.Series]::new(
                    $definition.Label)
            $series.ChartType = $definition.ChartType
            $series.ChartArea = 'main'
            $series.Legend = 'series'
            $series.Color = [System.Drawing.ColorTranslator]::FromHtml($definition.Color)
            $series.BorderWidth = 3
            $series.BorderDashStyle = $definition.Dash
            $series.MarkerStyle = $definition.Marker
            $series.MarkerSize = 9
            $series.MarkerColor = $series.Color
            $selected = @(Get-BenchmarkRows $definition.Workload `
                    $definition.Operation $definition.Structure)
            foreach ($row in $selected) {
                $value = Read-Number $row $definition.Metric
                if ($timePlot) { $value = $value / 1000000.0 }
                if ($logY -and $value -le 0) {
                    throw "Cannot draw zero on logarithmic axis: $($definition.Label)"
                }
                if ($value -gt 0 -and $value -lt $minimumPositive) {
                    $minimumPositive = $value
                }
                if ($value -gt $maximumValue) { $maximumValue = $value }
                $series.Points.AddXY([double][int]$row.n, $value) | Out-Null
            }
            $chart.Series.Add($series) | Out-Null
        }

        if ($logY) {
            $lower = [math]::Pow(10, [math]::Floor([math]::Log10($minimumPositive)))
            $upper = [math]::Pow(10, [math]::Ceiling([math]::Log10($maximumValue)))
            if ($lower -ge $minimumPositive) { $lower = $lower / 10 }
            if ($upper -le $maximumValue) { $upper = $upper * 10 }
            $area.AxisY.Minimum = $lower
            $area.AxisY.Maximum = $upper
        }

        $chart.SaveImage((Join-Path $plotDir $fileName),
                [System.Windows.Forms.DataVisualization.Charting.ChartImageFormat]::Png)
    } finally {
        $chart.Dispose()
    }
}

New-Item -ItemType Directory -Path $plotDir -Force | Out-Null
$blue = '#2563EB'
$orange = '#D97706'
$purple = '#7C3AED'
$green = '#15803D'

Save-Plot 'time_random_access.png' 'Random access: 10,000 get calls' `
        'Average total time (ms, log scale)' @(
    (Series-Definition 'DynamicArray' 'random_access' 'get' 'DynamicArray' 'avg_time_ns' $blue),
    (Series-Definition 'LinkedList' 'random_access' 'get' 'LinkedList' 'avg_time_ns' $orange)
) $true $true

Save-Plot 'time_search.png' 'Search: 1,000 contains calls' `
        'Average total time (ms, log scale)' @(
    (Series-Definition 'DynamicArray' 'search' 'contains' 'DynamicArray' 'avg_time_ns' $blue),
    (Series-Definition 'LinkedList' 'search' 'contains' 'LinkedList' 'avg_time_ns' $orange)
) $true $true

foreach ($position in @('front', 'middle')) {
    $insert = "insert_$position"
    $remove = "remove_$position"
    Save-Plot "time_$position.png" "Insertion and removal at $position`: 1,000 calls" `
            'Average total time (ms, log scale)' @(
        (Series-Definition 'DynamicArray insert' 'insertion_removal' $insert 'DynamicArray' 'avg_time_ns' $blue),
        (Series-Definition 'DynamicArray remove' 'insertion_removal' $remove 'DynamicArray' 'avg_time_ns' $blue 'Dash'),
        (Series-Definition 'LinkedList insert' 'insertion_removal' $insert 'LinkedList' 'avg_time_ns' $orange),
        (Series-Definition 'LinkedList remove' 'insertion_removal' $remove 'LinkedList' 'avg_time_ns' $orange 'Dash')
    ) $true $true
}

Save-Plot 'time_priority.png' 'Priority processing: n operations per phase' `
        'Average total time (ms, log scale)' @(
    (Series-Definition 'MinHeap insert' 'priority_processing' 'insert' 'MinHeap' 'avg_time_ns' $blue),
    (Series-Definition 'MinHeap extractMin' 'priority_processing' 'extract_min' 'MinHeap' 'avg_time_ns' $orange)
) $true $true

Save-Plot 'counts_random_access.png' 'Random access: element accesses and node visits' `
        'Average count for 10,000 get calls (log scale)' @(
    (Series-Definition 'DynamicArray element accesses' 'random_access' 'get' 'DynamicArray' 'avg_element_accesses' $blue),
    (Series-Definition 'LinkedList node visits' 'random_access' 'get' 'LinkedList' 'avg_node_visits' $orange)
) $false $true

Save-Plot 'counts_search.png' 'Search: value comparisons' `
        'Average comparisons for 1,000 contains calls (log scale)' @(
    (Series-Definition 'DynamicArray comparisons' 'search' 'contains' 'DynamicArray' 'avg_comparisons' $blue),
    (Series-Definition 'LinkedList comparisons' 'search' 'contains' 'LinkedList' 'avg_comparisons' $orange 'Solid' 'Point' 'Diamond')
) $false $true 'For contains, comparisons equal element accesses or node visits.'

foreach ($operation in @('insert_front', 'remove_front', 'insert_middle', 'remove_middle')) {
    $label = $operation.Replace('_', ' ')
    $note = ''
    $logY = $true
    if ($operation -eq 'insert_front') {
        $logY = $false
        $note = 'LinkedList node visits are zero for this operation at every n.'
    }
    Save-Plot "counts_$operation.png" "Insertion and removal: $label, 1,000 calls" `
            $(if ($logY) { 'Average count (log scale)' } else { 'Average count' }) @(
        (Series-Definition 'DynamicArray element accesses' 'insertion_removal' $operation 'DynamicArray' 'avg_element_accesses' $blue),
        (Series-Definition 'DynamicArray movements' 'insertion_removal' $operation 'DynamicArray' 'avg_movements' $purple 'Dash' 'Line' 'Square'),
        (Series-Definition 'LinkedList node visits' 'insertion_removal' $operation 'LinkedList' 'avg_node_visits' $orange 'Solid' 'Point' 'Diamond')
    ) $false $logY $note
}

Save-Plot 'counts_priority.png' 'Priority processing: value comparisons' `
        'Average comparisons for n operations (log scale)' @(
    (Series-Definition 'MinHeap insert' 'priority_processing' 'insert' 'MinHeap' 'avg_comparisons' $blue),
    (Series-Definition 'MinHeap extractMin' 'priority_processing' 'extract_min' 'MinHeap' 'avg_comparisons' $orange)
) $false $true

Write-Output "Validated $($rows.Count) rows and saved 12 plots to $plotDir"
